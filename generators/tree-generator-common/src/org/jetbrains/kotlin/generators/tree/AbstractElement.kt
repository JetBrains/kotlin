/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

/**
 * A class representing a FIR or IR tree element.
 */
abstract class AbstractElement<Element, Field> : ElementOrRef<Element, Field>, FieldContainer, ImplementationKindOwner
        where Element : AbstractElement<Element, Field>,
              Field : AbstractField {

    abstract val name: String

    /**
     * The fully-qualified name of the property in the tree generator that is used to configure this element.
     */
    abstract val propertyName: String

    abstract val kDoc: String?

    abstract val fields: Set<Field>

    abstract val params: List<TypeVariable>

    abstract val elementParents: List<ElementRef<Element, Field>>

    abstract val otherParents: MutableList<ClassRef<*>>

    val parentRefs: List<ClassOrElementRef>
        get() = elementParents + otherParents

    val isRootElement: Boolean
        get() = elementParents.isEmpty()

    open val isSealed: Boolean
        get() = false

    override val allParents: List<Element>
        get() = elementParents.map { it.element }

    context(ImportCollector)
    final override fun renderTo(appendable: Appendable) {
        addImport(this)
        appendable.append(typeName)
        if (params.isNotEmpty()) {
            params.joinTo(appendable, prefix = "<", postfix = ">") { it.name }
        }
    }

    abstract override val allFields: List<Field>

    abstract override val walkableChildren: List<Field>

    abstract override val transformableChildren: List<Field>

    final override fun get(fieldName: String): Field? {
        return allFields.firstOrNull { it.name == fieldName }
    }

    @Suppress("UNCHECKED_CAST")
    final override fun copy(nullable: Boolean) =
        ElementRef(this as Element, args, nullable)

    @Suppress("UNCHECKED_CAST")
    final override fun copy(args: Map<NamedTypeParameterRef, TypeRef>) =
        ElementRef(this as Element, args, nullable)

    @Suppress("UNCHECKED_CAST")
    override fun substitute(map: TypeParameterSubstitutionMap): Element = this as Element

    fun withStarArgs(): ElementRef<Element, Field> = copy(params.associateWith { TypeRef.Star })
}
