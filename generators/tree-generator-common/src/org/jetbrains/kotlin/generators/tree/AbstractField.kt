/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.imports.Importable

abstract class AbstractField<Field : AbstractField<Field>> {

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

    open var optInAnnotation: PrintableAnnotation? = null
    open var replaceOptInAnnotation: PrintableAnnotation? = null

    abstract var isMutable: Boolean

    open var customInitializationCall: String? = null

    val invisibleField: Boolean
        get() = customInitializationCall != null

    var deprecation: Deprecated? = null

    var visibility: Visibility = Visibility.PUBLIC

    var isOverride: Boolean = false

    var doPrint = true

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

    // TODO (marco): Name? `CustomImplementation`? `CustomImplementingField`? `CustomInternalRepresentation`?
    open var customRepresentation: CustomRepresentation<Field>? = null

    // TODO (marco): IMPORTANT: Awkward, awkward: Exposing the symbol ID would be good after all, because going
    //  `fir.referencedSymbol.symbolId` might require a symbol restoration, even if the use site only needs the symbol ID. Accessing
    //  implementation classes to get this symbol ID is not an option.
    //  So, back to the drawing board or do we disregard this until someone actual needs a symbol ID this way? Even if we want to expose the
    //  symbol ID, we would have to implement some custom logic for the builder, as we want people to specify the symbol, not the symbol ID,
    //  or at least allow specifying either.
    //
    // TODO (marco): Document, especially why this is different from the implementation default strategy.
    //  Also, move the class down?
    //
    // TODO (marco): Consider rolling this into `ImplementationDefaultStrategy` and rename the class to `ImplementationStrategy`. The
    //  lateinit, default, and custom representation strategies are all mutually exclusive, and they all define how the field should be
    //  implemented. When a custom representation is set, `implementationDefaultStrategy` should be `null`, which is a strong sign that they
    //  should be rolled into one concept as well.
    //  ...
    //  Furthermore, generating a getter for a field is a bit convoluted because both the implementation strategy and the custom
    //  representation generate getters. Here again, it feels like we are duplicating concerns.
    //  ...
    //  That said, we will have to consider the inheritance of the implementation strategy, as noted in `implementationDefaultStrategy`.
    //  Although we should have the same problems with the custom representation, since only leaf classes should generate the custom
    //  representation field, but if configured in parent classes, the concept should pass down to child classes. So this might be another
    //  hint that the concepts should be merged.
    //
    // TODO (marco): Another possible alternative would be to use two separate fields, with a default implementation for the symbol, but
    //  with flags that control whether the field is visible in: the surface, the builder, the implementation. The symbol ID field could be
    //  implemented as `!surface && !builder && implementation` (we can derive `override = false` from `!surface`). The symbol field could
    //  be implemented as all three being true, but with a default getter. The only blind spot here would be how to translate the symbol to
    //  the symbol ID in the builder. (Possible solution: when `!builder && implementation` is specified, the use site has to specify a
    //  custom builder expression. Or in general, we allow specifying a custom builder expression.)
    //  With this solution, we add a few orthogonal features that give us the freedom to generate the classes how we want.
    //  Probably `surface` should rather be `interface`.
    //
    // TODO (marco): In any case, document this class.
    //  From earlier:
    //  So the custom representation differs from the default value approach: A field with a custom representation should still appear as a
    //  builder property, while a field with a default/computed value does not require any mention in the builder. The value of the custom
    //  representation is derived from the public field in the builder, while the computed value is derived from an expression/another field at
    //  access time.
    class CustomRepresentation<Field : AbstractField<Field>>(
        // TODO (marco): Should this be copied or not?
        val field: Field,

        /**
         * Generates an expression which converts the *original field* to the *custom representation*.
         */
        val toRepresentation: (Field) -> String,

        /**
         * Generates an expression which converts the *custom representation field* to the *original value*.
         */
        val fromRepresentation: (Field) -> String,
    ) {
        init {
            // TODO (marco): Maybe not the best place to configure this?
            field.isCustomRepresentation = true
            field.visibility = Visibility.PRIVATE
        }
    }

    /**
     * Whether *this* field is a [CustomRepresentation] of another field, i.e. this field is contained in the [CustomRepresentation] class.
     *
     * This property is set automatically when a [CustomRepresentation] is created.
     */
    var isCustomRepresentation: Boolean = false

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
        copy.customRepresentation = customRepresentation
        copy.isCustomRepresentation = isCustomRepresentation
        copy.doPrint = doPrint
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
