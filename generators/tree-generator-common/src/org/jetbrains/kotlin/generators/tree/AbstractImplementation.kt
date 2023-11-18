/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

/**
 * A class representing a non-abstract implementation of an abstract class/interface of a tree node.
 */
@Suppress("LeakingThis")
abstract class AbstractImplementation<Implementation, Element, Field>(
    val element: Element,
    val name: String?,
) : FieldContainer<Field>, ImplementationKindOwner
        where Implementation : AbstractImplementation<Implementation, Element, Field>,
              Element : AbstractElement<Element, *, Implementation>,
              Field : AbstractField<*> {

    private val isDefault: Boolean
        get() = name == null

    override val allParents: List<ImplementationKindOwner>
        get() = listOf(element)

    override val typeName: String = name ?: (element.typeName + "Impl")

    context(ImportCollector)
    override fun renderTo(appendable: Appendable) {
        addImport(this)
        appendable.append(this.typeName)
        if (element.params.isNotEmpty()) {
            element.params.joinTo(appendable, prefix = "<", postfix = ">") { it.name }
        }
    }

    override fun substitute(map: TypeParameterSubstitutionMap) = this

    override val packageName = element.packageName + ".impl"

    val usedTypes = mutableListOf<Importable>()

    init {
        @Suppress("UNCHECKED_CAST")
        if (isDefault) {
            element.defaultImplementation = this as Implementation
        } else {
            element.customImplementations += this as Implementation
        }
    }

    override val hasAcceptChildrenMethod: Boolean
        get() {
            val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
            val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass
            return !isInterface && !isAbstract
        }

    override val hasTransformChildrenMethod: Boolean
        get() = true
    var isPublic = false

    override fun get(fieldName: String): Field? {
        return allFields.firstOrNull { it.name == fieldName }
    }

    private fun withDefault(field: Field) = !field.isFinal && (field.defaultValueInImplementation != null || field.isLateinit)

    val fieldsWithoutDefault by lazy { allFields.filterNot(::withDefault) }

    val fieldsWithDefault by lazy { allFields.filter(::withDefault) }

    var requiresOptIn = false

    override var kind: ImplementationKind? = null
        set(value) {
            field = value
            if (kind != ImplementationKind.FinalClass) {
                isPublic = true
            }
            @Suppress("UNCHECKED_CAST")
            builder = if (value?.hasLeafBuilder == true) {
                builder ?: LeafBuilder(this as Implementation)
            } else {
                null
            }
        }

    var builder: LeafBuilder<Field, Element, Implementation>? = null
}