/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.fir.tree.generator.printer.BASE_PACKAGE
import org.jetbrains.kotlin.fir.tree.generator.printer.safeDecapitalizedName
import org.jetbrains.kotlin.fir.tree.generator.util.set
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

class Element(override val name: String, override val propertyName: String, kind: Kind) : AbstractElement<Element, Field>() {
    companion object {
        private val allowedKinds = setOf(
            ImplementationKind.Interface,
            ImplementationKind.SealedInterface,
            ImplementationKind.AbstractClass,
            ImplementationKind.SealedClass
        )
    }

    override val element: Element
        get() = this

    override val args: Map<NamedTypeParameterRef, TypeRef>
        get() = emptyMap()

    override val nullable: Boolean
        get() = false

    override var kDoc: String? = null

    override val fields = mutableSetOf<Field>()
    override val typeName: String = "Fir$name"

    override val packageName: String = BASE_PACKAGE + kind.packageName.let { if (it.isBlank()) it else "." + it }

    override val elementParents = mutableListOf<ElementRef>()

    override val otherParents = mutableListOf<ClassRef<*>>()

    var defaultImplementation: Implementation? = null
    val customImplementations = mutableListOf<Implementation>()

    override val params = mutableListOf<TypeVariable>()

    override var kind: ImplementationKind? = null
        set(value) {
            if (value !in allowedKinds) {
                throw IllegalArgumentException(value.toString())
            }
            field = value
        }
    var _needTransformOtherChildren: Boolean = false

    override var isSealed: Boolean = false

    override val hasAcceptMethod: Boolean
        get() = true

    override val hasTransformMethod: Boolean
        get() = true

    override val hasAcceptChildrenMethod: Boolean
        get() = isRootElement

    override val hasTransformChildrenMethod: Boolean
        get() = isRootElement

    override val walkableChildren: List<Field>
        get() = emptyList() // Use Implementation#walkableChildren instead

    override val transformableChildren: List<Field>
        get() = emptyList() // Use Implementation#transformableChildren instead

    var baseTransformerType: Element? = null
    val transformerClass: Element get() = baseTransformerType ?: this

    override val visitFunctionName: String
        get() = "visit$name"

    override val visitorParameterName: String
        get() = safeDecapitalizedName

    var customParentInVisitor: Element? = null

    override val parentInVisitor: Element?
        get() = customParentInVisitor ?: elementParents.singleOrNull()?.element?.takeIf { !it.isRootElement }

    var doesNotNeedImplementation: Boolean = false

    val needTransformOtherChildren: Boolean get() = _needTransformOtherChildren || elementParents.any { it.element.needTransformOtherChildren }
    val overridenFields: MutableMap<Field, MutableMap<Field, Boolean>> = mutableMapOf()
    val useNullableForReplace: MutableSet<Field> = mutableSetOf()
    val allImplementations: List<Implementation> by lazy {
        if (doesNotNeedImplementation) {
            emptyList()
        } else {
            val implementations = customImplementations.toMutableList()
            defaultImplementation?.let { implementations += it }
            implementations
        }
    }

    override val allFields: List<Field> by lazy {
        val result = LinkedHashSet<Field>()
        result.addAll(fields.toList().asReversed())
        result.forEach { overridenFields[it, it] = false }
        for (parentField in parentFields.asReversed()) {
            val overrides = !result.add(parentField)
            if (overrides) {
                val existingField = result.first { it == parentField }
                existingField.fromParent = true
                existingField.needsSeparateTransform = existingField.needsSeparateTransform || parentField.needsSeparateTransform
                existingField.needTransformInOtherChildren = existingField.needTransformInOtherChildren || parentField.needTransformInOtherChildren
                existingField.withReplace = parentField.withReplace || existingField.withReplace
                existingField.parentHasSeparateTransform = parentField.needsSeparateTransform
                if (parentField.typeRef.copy(nullable = false) != existingField.typeRef.copy(nullable = false) && parentField.withReplace) {
                    existingField.overridenTypes += parentField.typeRef
                    overridenFields[existingField, parentField] = false
                } else {
                    overridenFields[existingField, parentField] = true
                    if (parentField.nullable != existingField.nullable) {
                        existingField.useNullableForReplace = true
                    }
                }
            } else {
                overridenFields[parentField, parentField] = true
            }
        }
        result.toList().asReversed()
    }

    val parentFields: List<Field> by lazy {
        val result = LinkedHashMap<String, Field>()
        elementParents.forEach { parentRef ->
            val parent = parentRef.element
            val fields = parent.allFields.map { field ->
                val copy = (field as? SimpleField)?.let { simpleField ->
                    simpleField.replaceType(simpleField.typeRef.substitute(parentRef.args) as TypeRefWithNullability)
                } ?: field.copy()
                copy.apply {
                    fromParent = true
                }
            }
            fields.forEach {
                result.merge(it.name, it) { previousField, thisField ->
                    val resultField = previousField.copy()
                    if (thisField.withReplace) {
                        resultField.withReplace = true
                    }
                    if (thisField.useNullableForReplace) {
                        resultField.useNullableForReplace = true
                    }
                    if (thisField.isMutable) {
                        resultField.isMutable = true
                    }
                    resultField
                }
            }
        }
        result.values.toList()
    }

    val allFirFields: List<Field> by lazy {
        allFields.filter { it.isFirType }
    }

    override fun toString(): String {
        return with(ImportCollector("")) { render() }
    }

    enum class Kind(val packageName: String) {
        Expression("expressions"),
        Declaration("declarations"),
        Reference("references"),
        TypeRef("types"),
        Contracts("contracts"),
        Diagnostics("diagnostics"),
        Other("")
    }
}

typealias ElementRef = GenericElementRef<Element, Field>

typealias ElementOrRef = GenericElementOrRef<Element, Field>
