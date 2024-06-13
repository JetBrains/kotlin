/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.imports.Importable

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

    override val allParents: List<Element>
        get() = listOf(element)

    val namePrefix: String
        get() = element.namePrefix

    override val typeName: String
        get() = name ?: (element.typeName + "Impl")

    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        importCollector.addImport(this)
        appendable.append(this.typeName)
        if (element.params.isNotEmpty()) {
            element.params.joinTo(appendable, prefix = "<", postfix = ">") { it.name }
        }
    }

    override fun substitute(map: TypeParameterSubstitutionMap) = this

    override val packageName = element.packageName + ".impl"

    /**
     * Types/functions that you want to additionally import in the file with the implementation class.
     *
     * This is useful if, for example, default values of fields reference classes or functions from other packages.
     *
     * Note that classes referenced in field types will be imported automatically.
     */
    val additionalImports = mutableListOf<Importable>()

    var kDoc: String? = null

    init {
        @Suppress("UNCHECKED_CAST")
        element.implementations += this as Implementation
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
    var isConstructorPublic = true

    var putImplementationOptInInConstructor = true

    var constructorParameterOrderOverride: List<String>? = null

    private fun withDefault(field: Field) =
        !field.isFinal && field.implementationDefaultStrategy !is AbstractField.ImplementationDefaultStrategy.Required

    val fieldsInConstructor by lazy { allFields.filterNot(::withDefault) }

    val fieldsInBody by lazy { allFields.filter(::withDefault) }

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

    open val doPrint: Boolean
        get() = true

    override fun toString(): String = buildString { renderTo(this, ImportCollecting.Empty) }
}
