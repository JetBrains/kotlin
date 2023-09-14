/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.fir.tree.generator.printer.BASE_PACKAGE
import org.jetbrains.kotlin.fir.tree.generator.util.set
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

class Element(override val name: String, kind: Kind) : AbstractElement<Element, Field> {
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

    override fun copy(nullable: Boolean) = ElementRef(this, args, nullable)

    override fun copy(args: Map<NamedTypeParameterRef, TypeRef>) = ElementRef(this, args, nullable)

    override val fields = mutableSetOf<Field>()
    override val type: String = "Fir$name"
    override val packageName: String = BASE_PACKAGE + kind.packageName.let { if (it.isBlank()) it else "." + it }
    override val fullQualifiedName: String get() = super.fullQualifiedName!!
    override val parents = mutableListOf<Element>()
    var defaultImplementation: Implementation? = null
    val customImplementations = mutableListOf<Implementation>()

    override val params = mutableListOf<TypeVariable>()

    override val parentsArguments = mutableMapOf<Element, MutableMap<TypeRef, TypeRef>>()
    override var kind: ImplementationKind? = null
        set(value) {
            if (value !in allowedKinds) {
                throw IllegalArgumentException(value.toString())
            }
            field = value
        }
    var _needTransformOtherChildren: Boolean = false

    override var isSealed: Boolean = false

    var baseTransformerType: Element? = null
    val transformerType: Element get() = baseTransformerType ?: this

    var doesNotNeedImplementation: Boolean = false

    val needTransformOtherChildren: Boolean get() = _needTransformOtherChildren || parents.any { it.needTransformOtherChildren }
    override val overridenFields: MutableMap<Field, MutableMap<Field, Boolean>> = mutableMapOf()
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
                if (parentField.type != existingField.type && parentField.withReplace) {
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
        parents.forEach { parent ->
            val fields = parent.allFields.map { field ->
                val copy = (field as? SimpleField)?.let { simpleField ->
                    parentsArguments[parent]?.get(NamedTypeParameterRef(simpleField.type))?.let {
                        simpleField.replaceType(it)
                    }
                } ?: field.copy()
                copy.apply {
                    arguments.replaceAll {
                        parentsArguments[parent]?.get(it) ?: it
                    }
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
        return typeWithArguments
    }

    override fun get(fieldName: String): Field? {
        return allFields.firstOrNull { it.name == fieldName }
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
