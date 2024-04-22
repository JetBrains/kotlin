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

    var fromParent: Boolean = false

    /**
     * Whether this field can contain an element either directly or e.g. in a list.
     */
    open val containsElement: Boolean
        get() = typeRef is ElementOrRef<*> || this is ListField && baseType is ElementOrRef<*>

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

    /**
     * Whether this field semantically represents a reference to a child node of the tree.
     *
     * This may have the effect of including or excluding this field from visiting it by visitors in the generated
     * `acceptChildren` and `transformChildren` methods (child fields are always visited in those methods).
     *
     * Only has effect if [containsElement] is `true`.
     */
    abstract val isChild: Boolean

    open val overriddenTypes: MutableSet<TypeRefWithNullability> = mutableSetOf()

    open fun updatePropertiesFromOverriddenField(parentField: Field, haveSameClass: Boolean) {}

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        other as AbstractField<*>
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    /**
     * Returns a copy of this field with its [typeRef] set to [newType] (if it's possible).
     */
    abstract fun replaceType(newType: TypeRefWithNullability): Field

    /**
     * Returns a copy of this field.
     */
    abstract fun copy(): Field

    protected open fun updateFieldsInCopy(copy: Field) {
        copy.kDoc = kDoc
        copy.arbitraryImportables += arbitraryImportables
        copy.optInAnnotation = optInAnnotation
        copy.replaceOptInAnnotation = replaceOptInAnnotation
        copy.isMutable = isMutable
        copy.deprecation = deprecation
        copy.visibility = visibility
        copy.fromParent = fromParent
        copy.useInBaseTransformerDetection = useInBaseTransformerDetection
        copy.overriddenTypes += overriddenTypes
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

    /**
     * If this field represents a symbol of a declaration ([org.jetbrains.kotlin.ir.symbols.IrSymbol] or
     * [org.jetbrains.kotlin.fir.symbols.FirBasedSymbol]), determines whether this symbol corresponds to the element containing this field
     * or some other element.
     *
     * In other words, for element `someElement` the following is true:
     * [symbolFieldRole] == [SymbolFieldRole.DECLARED] iff `someElement.symbol.owner === someElement`.
     *
     * If this field does not represent a symbol, this property should be `null`.
     */
    var symbolFieldRole: SymbolFieldRole? = null

    enum class SymbolFieldRole {
        DECLARED, REFERENCED
    }
}
