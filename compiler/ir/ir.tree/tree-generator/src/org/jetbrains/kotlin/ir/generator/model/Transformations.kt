/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.config.*
import org.jetbrains.kotlin.ir.generator.elementBaseType
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.castAll
import org.jetbrains.kotlin.utils.addToStdlib.partitionIsInstance
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

private object InferredOverriddenType : TypeRefWithNullability {
    override val type: String
        get() = error("not supported")
    override val packageName: String?
        get() = null

    override val typeWithArguments: String
        get() = error("not supported")

    override fun substitute(map: TypeParameterSubstitutionMap) = this

    override val nullable: Boolean
        get() = false

    override fun copy(nullable: Boolean) = this
}

data class Model(val elements: List<Element>, val rootElement: Element)

fun config2model(config: Config): Model {
    val ec2el = mutableMapOf<ElementConfig, Element>()

    val elements = config.elements.map { ec ->
        Element(
            config = ec,
            name = ec.name,
            packageName = ec.category.packageName,
            params = ec.params,
            fields = ec.fields.mapTo(mutableSetOf(), ::transformFieldConfig),
            additionalFactoryMethodParameters = ec.additionalIrFactoryMethodParameters.mapTo(mutableListOf(), ::transformFieldConfig)
        ).also {
            ec2el[ec.element] = it
        }
    }

    val rootElement = replaceElementRefs(config, ec2el)
    configureInterfacesAndAbstractClasses(elements)
    addPureAbstractElement(elements, elementBaseType)
    markLeaves(elements)
    configureDescriptorApiAnnotation(elements)
    processFieldOverrides(elements)
    addWalkableChildren(elements)

    return Model(elements, rootElement)
}

private fun transformFieldConfig(fc: FieldConfig): Field = when (fc) {
    is SimpleFieldConfig -> SingleField(
        fc,
        fc.name,
        fc.type?.copy(fc.nullable) ?: InferredOverriddenType,
        fc.mutable,
        fc.isChild,
        fc.baseDefaultValue,
        fc.baseGetter,
    )
    is ListFieldConfig -> {
        val listType = when (fc.mutability) {
            ListFieldConfig.Mutability.List -> StandardTypes.mutableList
            ListFieldConfig.Mutability.Array -> StandardTypes.array
            else -> StandardTypes.list
        }
        ListField(
            fc,
            fc.name,
            fc.elementType ?: InferredOverriddenType,
            listType.copy(fc.nullable),
            fc.mutability == ListFieldConfig.Mutability.Var,
            fc.isChild,
            fc.mutability != ListFieldConfig.Mutability.Immutable,
            fc.baseDefaultValue,
            fc.baseGetter,
        )
    }
}

@OptIn(UnsafeCastFunction::class)
@Suppress("UNCHECKED_CAST")
private fun replaceElementRefs(config: Config, mapping: Map<ElementConfig, Element>): Element {
    val visited = mutableMapOf<TypeRef, TypeRef>()

    fun transform(type: TypeRef): TypeRef {
        visited[type]?.let {
            return it
        }

        return when (type) {
            is ElementConfigOrRef -> {
                val args = type.args.mapValues { transform(it.value) }
                val el = mapping.getValue(type.element)
                ElementRef(el, args, type.nullable)
            }
            is ClassRef<*> -> {
                @Suppress("UNCHECKED_CAST") // this is the upper bound, compiler could know that, right?
                type as ClassRef<TypeParameterRef>

                val args = type.args.mapValues { transform(it.value) }
                type.copy(args = args)
            }
            else -> type
        }.also { visited[type] = it }
    }

    val rootEl = transform(config.rootElement) as GenericElementRef<Element, Field>

    for (ec in config.elements) {
        val el = mapping[ec.element]!!
        val (elParents, otherParents) = ec.parents
            .map { transform(it) }
            .partitionIsInstance<TypeRef, ElementRef>()
        el.elementParents = elParents.takeIf { it.isNotEmpty() || el == rootEl.element } ?: listOf(rootEl)
        el.otherParents = otherParents.castAll<ClassRef<*>>().toMutableList()
        el.visitorParent = ec.visitorParent?.let(::transform) as GenericElementRef<Element, Field>?
        el.transformerReturnType = (ec.transformerReturnType?.let(::transform) as GenericElementRef<Element, Field>?)?.element

        for (field in el.fields) {
            when (field) {
                is SingleField -> {
                    field.typeRef = transform(field.typeRef) as TypeRefWithNullability
                }
                is ListField -> {
                    field.elementType = transform(field.elementType)
                }
            }
        }
    }

    return rootEl.element
}

private fun markLeaves(elements: List<Element>) {
    val leaves = elements.toMutableSet()

    for (el in elements) {
        for (parent in el.elementParents) {
            if (!parent.element.isLeaf) {
                leaves.remove(parent.element)
            }
        }
    }

    for (el in leaves) {
        el.isLeaf = true
        if (el.visitorParent != null) {
            el.hasAcceptMethod = true
        }
    }
}

private fun configureDescriptorApiAnnotation(elements: List<Element>) {
    for (el in elements) {
        for (field in el.fields) {
            val type = field.typeRef
            if (type is ClassRef<*> && type.packageName.startsWith("org.jetbrains.kotlin.descriptors") &&
                type.simpleName.endsWith("Descriptor") && type.simpleName != "ModuleDescriptor"
            ) {
                field.needsDescriptorApiAnnotation = true
            }
        }
    }
}

private fun processFieldOverrides(elements: List<Element>) {
    for (element in iterateElementsParentFirst(elements)) {
        for (field in element.fields) {
            fun visitParents(visited: Element) {
                for (parent in visited.elementParents) {
                    val overriddenField = parent.element.fields.singleOrNull { it.name == field.name }
                    if (overriddenField != null) {
                        field.isOverride = true
                        field.needsDescriptorApiAnnotation =
                            field.needsDescriptorApiAnnotation || overriddenField.needsDescriptorApiAnnotation

                        fun transformInferredType(type: TypeRef, overriddenType: TypeRef) =
                            type.takeUnless { it is InferredOverriddenType } ?: overriddenType
                        when (field) {
                            is SingleField -> {
                                field.typeRef =
                                    transformInferredType(field.typeRef, (overriddenField as SingleField).typeRef) as TypeRefWithNullability
                            }
                            is ListField -> {
                                field.elementType = transformInferredType(field.elementType, (overriddenField as ListField).elementType)
                            }
                        }

                        break
                    }

                    visitParents(parent.element)
                }
            }

            visitParents(element)
        }
    }
}

private fun addWalkableChildren(elements: List<Element>) {
    for (element in elements) {
        val walkableChildren = mutableMapOf<String, Field>()

        fun visitParents(visited: Element) {
            for (parent in visited.elementParents) {
                if (!parent.element.ownsChildren) {
                    for (field in parent.element.fields) {
                        if (field.isChild) {
                            walkableChildren[field.name] = field
                        }
                    }

                    visitParents(parent.element)
                }
            }
        }

        visitParents(element)

        element.fields.filter { it.isChild }.associateByTo(walkableChildren) { it.name }

        element.walkableChildren = reorderIfNecessary(walkableChildren.values.toList(), element.childrenOrderOverride)
    }
}

private fun reorderIfNecessary(fields: List<Field>, order: List<String>?): List<Field> =
    if (order == null) fields else fields.sortedBy {
        val position = order.indexOf(it.name)
        if (position < 0) order.size else position
    }

private fun iterateElementsParentFirst(elements: List<Element>) = sequence {
    val pending = elements.sortedBy { it.elementParents.size }.toMutableSet()
    pendingLoop@ while (pending.isNotEmpty()) {
        val iter = pending.iterator()
        while (iter.hasNext()) {
            val element = iter.next()
            if (element.elementParents.none { it.element in pending }) {
                yield(element)
                iter.remove()
                continue@pendingLoop
            }
        }

        error("Cannot find next element to process")
    }
}
