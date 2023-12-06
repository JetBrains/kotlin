/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

/**
 * Represents an abstract class/interface for a tree node in the tree generator.
 *
 * Examples: `IrElement`, `FirRegularClass`.
 *
 * Subclasses may contain additional properties/logic specific to a particular tree.
 */
abstract class AbstractElement<Element, Field, Implementation>(
    val name: String,
) : ElementOrRef<Element>, FieldContainer<Field>, ImplementationKindOwner
        where Element : AbstractElement<Element, Field, Implementation>,
              Field : AbstractField<Field>,
              Implementation : AbstractImplementation<Implementation, Element, *> {

    /**
     * The fully-qualified name of the property in the tree generator that is used to configure this element.
     */
    abstract val propertyName: String

    abstract val namePrefix: String

    abstract val kDoc: String?

    abstract val fields: Set<Field>

    abstract val params: List<TypeVariable>

    abstract val elementParents: List<ElementRef<Element>>

    abstract val otherParents: MutableList<ClassRef<*>>

    val parentRefs: List<ClassOrElementRef>
        get() = elementParents + otherParents

    val isRootElement: Boolean
        get() = elementParents.isEmpty()

    var isSealed: Boolean = false

    /**
     * The value of this property will be used to name a `visit*` method for this element in visitor classes.
     *
     * In `visit*`, the `*` will be replaced with the value of this property.
     */
    var nameInVisitorMethod: String = name

    /**
     * The name of the method in visitors used to visit this element.
     */
    val visitFunctionName: String
        get() = "visit$nameInVisitorMethod"

    /**
     * The name of the parameter representing this element in the visitor method used to visit this element.
     */
    abstract val visitorParameterName: String

    /**
     * @see parentInVisitor
     */
    var customParentInVisitor: Element? = null

    /**
     * The default element to visit if the method for visiting this element is not overridden.
     */
    open val parentInVisitor: Element?
        get() = customParentInVisitor ?: elementParents.singleOrNull()?.element?.takeIf { !it.isRootElement }

    override val allParents: List<Element>
        get() = elementParents.map { it.element }

    override val typeName: String
        get() = namePrefix + name

    context(ImportCollector)
    final override fun renderTo(appendable: Appendable) {
        addImport(this)
        appendable.append(typeName)
        if (params.isNotEmpty()) {
            params.joinTo(appendable, prefix = "<", postfix = ">") { it.name }
        }
    }

    override val allFields: List<Field> by lazy {
        val result = LinkedHashSet<Field>()
        result.addAll(fields.toList().asReversed())
        result.forEach { overriddenFieldsHaveSameClass[it, it] = false }
        for (parentField in parentFields.asReversed()) {
            val overrides = !result.add(parentField)
            if (overrides) {
                val existingField = result.first { it == parentField }
                existingField.fromParent = true
                val haveSameClass = parentField.typeRef.copy(nullable = false) == existingField.typeRef.copy(nullable = false)
                if (!haveSameClass) {
                    existingField.overriddenTypes += parentField.typeRef
                }
                overriddenFieldsHaveSameClass[existingField, parentField] = haveSameClass
                existingField.updatePropertiesFromOverriddenField(parentField, haveSameClass)
            } else {
                overriddenFieldsHaveSameClass[parentField, parentField] = true
            }
        }
        result.toList().asReversed()
    }

    val overriddenFieldsHaveSameClass: MutableMap<Field, MutableMap<Field, Boolean>> = mutableMapOf()

    val parentFields: List<Field> by lazy {
        val result = LinkedHashMap<String, Field>()
        elementParents.forEach { parentRef ->
            val parent = parentRef.element
            val fields = parent.allFields.map { field ->
                field.replaceType(field.typeRef.substitute(parentRef.args) as TypeRefWithNullability)
                    .apply {
                        fromParent = true
                    }
            }
            fields.forEach {
                result.merge(it.name, it) { previousField, thisField ->
                    val resultField = previousField.copy()
                    if (thisField.isMutable) {
                        resultField.isMutable = true
                    }
                    resultField
                }
            }
        }
        result.values.toList()
    }

    /**
     * A custom return type of the corresponding transformer method for this element.
     */
    var transformerReturnType: Element? = null

    /**
     * @see org.jetbrains.kotlin.generators.tree.detectBaseTransformerTypes
     */
    internal var baseTransformerType: Element? = null

    /**
     * The return type of the corresponding transformer method for this element.
     *
     * By default, computed using [org.jetbrains.kotlin.generators.tree.detectBaseTransformerTypes], but can be customized via
     * [transformerReturnType]
     */
    val transformerClass: Element
        get() = transformerReturnType ?: baseTransformerType ?: element

    var defaultImplementation: Implementation? = null

    val customImplementations = mutableListOf<Implementation>()

    var doesNotNeedImplementation: Boolean = false

    val allImplementations: List<Implementation> by lazy {
        if (doesNotNeedImplementation) {
            emptyList()
        } else {
            val implementations = customImplementations.toMutableList()
            defaultImplementation?.let { implementations += it }
            implementations
        }
    }

    /**
     * Types/functions that you want to additionally import in the file with the element class.
     *
     * This is useful if, for example, default values of fields reference classes or functions from other packages.
     *
     * Note that classes referenced in field types will be imported automatically.
     */
    val additionalImports = mutableListOf<Importable>()

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

    fun withStarArgs(): ElementRef<Element> = copy(params.associateWith { TypeRef.Star })
}
