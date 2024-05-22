/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.imports.Importable

abstract class AbstractField<Field : AbstractField<Field>> {
    abstract val origin: Field

    abstract val name: String

    abstract val typeRef: TypeRefWithNullability

    val nullable: Boolean
        get() = typeRef.nullable

    var kDoc: String? = null

    open val isVolatile: Boolean
        get() = false

    abstract val isFinal: Boolean

    open val isParameter: Boolean
        get() = false

    open val arbitraryImportables: MutableList<Importable> = mutableListOf()

    open var optInAnnotation: ClassRef<*>? = null
    open var replaceOptInAnnotation: ClassRef<*>? = null

    abstract var isMutable: Boolean

    open var customInitializationCall: String? = null

    val invisibleField: Boolean
        get() = customInitializationCall != null

    var deprecation: Deprecated? = null

    var visibility: Visibility = Visibility.PUBLIC

    var isOverride: Boolean = false

    /**
     * If `true`, this field is skipped in `build%Element%Copy` functions.
     *
     *  @see AbstractBuilderPrinter.printDslBuildCopyFunction
     */
    open var skippedInCopy: Boolean = false

    /**
     * Whether this field can contain an element either directly or e.g. in a list.
     */
    open val containsElement: Boolean
        get() = typeRef is ElementOrRef<*> || this is ListField && baseType is ElementOrRef<*>

    val hasSymbolType: Boolean
        get() = (typeRef as? ClassRef<*>)?.simpleName?.contains("Symbol") ?: false

    /**
     * Indicates how the field will be initialized.
     *
     * Null value means that the initialization strategy has not been explicitly configured,
     * and it will be inherited from an ancestor element, or assigned a default strategy
     * of [ImplementationDefaultStrategy.Required].
     *
     * @see org.jetbrains.kotlin.generators.tree.config.AbstractImplementationConfigurator.inheritImplementationFieldSpecifications .
     */
    open var implementationDefaultStrategy: ImplementationDefaultStrategy? = null

    abstract var defaultValueInBuilder: String?

    abstract var customSetter: String?

    /**
     * @see org.jetbrains.kotlin.generators.tree.detectBaseTransformerTypes
     */
    var useInBaseTransformerDetection = true

    open val overriddenFields: MutableSet<Field> = mutableSetOf<Field>()

    open fun updatePropertiesFromOverriddenFields(parentFields: List<Field>) {
        overriddenFields += parentFields
        isMutable = isMutable || parentFields.any { it.isMutable }
    }

    override fun toString(): String {
        return name
    }

    /**
     * Replaces the type of the field with its substituted [TypeRef.substitute] version,
     * if it's possible.
     */
    abstract fun substituteType(map: TypeParameterSubstitutionMap)

    /**
     * Returns a copy of this field.
     */
    fun copy() = internalCopy().also(::updateFieldsInCopy)

    protected abstract fun internalCopy(): Field

    protected open fun updateFieldsInCopy(copy: Field) {
        copy.kDoc = kDoc
        copy.arbitraryImportables += arbitraryImportables
        copy.optInAnnotation = optInAnnotation
        copy.replaceOptInAnnotation = replaceOptInAnnotation
        copy.isMutable = isMutable
        copy.deprecation = deprecation
        copy.visibility = visibility
        copy.isOverride = isOverride
        copy.useInBaseTransformerDetection = useInBaseTransformerDetection
        copy.overriddenFields += overriddenFields
        copy.implementationDefaultStrategy = implementationDefaultStrategy
    }

    sealed interface ImplementationDefaultStrategy {
        open val defaultValue: String?
            get() = null
        open val withGetter: Boolean
            get() = false


        /**
         * The field will have to be initialized explicitly in the implementation class constructor.
         */
        data object Required : ImplementationDefaultStrategy

        /**
         * The field will be `lateinit var`.
         */
        data object Lateinit : ImplementationDefaultStrategy

        /**
         * - If [withGetter] == false - the field will be a stored property, initialized to [defaultValue].
         * - If [withGetter] == true - the field will be a computed property, with getter returning [defaultValue].
         */
        data class DefaultValue(
            override val defaultValue: String,
            override val withGetter: Boolean,
        ) : ImplementationDefaultStrategy
    }

    abstract var kind: Kind

    enum class Kind {
        /**
         * Arbitrary data, opaque to the framework.
         */
        RegularField,

        /**
         * A symbol ([org.jetbrains.kotlin.ir.symbols.IrSymbol] or
         * [org.jetbrains.kotlin.fir.symbols.FirBasedSymbol]) representing this declaration.
         * I.e. `someElement.symbol.owner === someElement`.
         */
        DeclaredSymbol,

        /**
         * A reference to some other element, or a list of elements, either direct
         * ([org.jetbrains.kotlin.ir.IrElement], [org.jetbrains.kotlin.fir.FirElement])
         * or via a symbol
         * ([org.jetbrains.kotlin.ir.symbols.IrSymbol], [org.jetbrains.kotlin.fir.symbols.FirBasedSymbol]).
         */
        ElementReference,

        /**
         * A reference to a child node or a list of child nodes of the tree.
         * Child elements have a special behavior:
         * - They are visited in `acceptChildren` and `transformChildren` methods.
         * - They may have `parent` property representing the element which contains them.
         * - In order to maintain the tree structure, every element object is expected to be present in at most one [ChildElement] field.
         */
        ChildElement,
    }
}
