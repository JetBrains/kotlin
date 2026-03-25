/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationUseSiteTarget
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo

@KaExperimentalApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaSymbolInformationProvider : KaSessionComponent {
    /**
     * The deprecation status of the given symbol, or `null` if the symbol is not deprecated.
     *
     * This considers deprecation annotations applied to the symbol itself and, for property-related
     * symbols, deprecation annotations with appropriate use-site targets.
     */
    @KaExperimentalApi
    public val KaSymbol.deprecation: KaDeprecation?

    /**
     * Whether the symbol is deprecated at any level ([WARNING][KaDeprecation.Level.WARNING], [ERROR][KaDeprecation.Level.ERROR],
     * or [HIDDEN][KaDeprecation.Level.HIDDEN]).
     *
     * This is a convenience property equivalent to `deprecation != null`.
     */
    @KaExperimentalApi
    public val KaSymbol.isDeprecated: Boolean

    /**
     * The deprecation status of the given symbol, or `null` if the declaration is not deprecated.
     */
    @KaExperimentalApi
    @Deprecated("Use 'deprecation' instead", level = DeprecationLevel.HIDDEN)
    @KaNoContextParameterBridgeRequired
    public val KaSymbol.deprecationStatus: DeprecationInfo?

    /**
     * Whether the function symbol meets all the requirements to be declared as an [operator function](https://kotlinlang.org/docs/operator-overloading.html).
     *
     * In Kotlin, the set of functions which can be declared as an operator is predefined. [canBeOperator] not only checks the name of a
     * potential operator function, but also its signature, depending on the operator.
     *
     * [canBeOperator] does not determine whether the function symbol *is* declared as an operator. For this purpose, use
     * [KaNamedFunctionSymbol.isOperator] instead.
     *
     * #### Example
     *
     * ```kotlin
     * class A
     *
     * fun A.plus(that: A): A = A() // canBeOperator = true, as it meets all requirements for `plus`.
     *
     * operator fun A.contains(that: A): Boolean = true // canBeOperator = true, as it's already an operator.
     *
     * fun A.something(that: A): A = A() // canBeOperator = false, as there is no operator with such a name.
     *
     * fun A.minus(): A = A() // canBeOperator = false, as `minus` is a binary operator and should have one parameter.
     * ```
     */
    @KaExperimentalApi
    public val KaNamedFunctionSymbol.canBeOperator: Boolean

    /**
     * The deprecation status of the given symbol for the given [annotation use-site target][useSiteTarget],
     * or `null` if the symbol is not deprecated for that target.
     *
     * @param useSiteTarget the annotation use-site target to check deprecation for
     */
    @KaExperimentalApi
    public fun KaSymbol.deprecation(useSiteTarget: KaAnnotationUseSiteTarget?): KaDeprecation?

    /**
     * The deprecation status of the given symbol for the given [annotation use-site target](https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets),
     * or `null` if the declaration is not deprecated.
     */
    @KaExperimentalApi
    @Deprecated("Use 'deprecation()' instead", level = DeprecationLevel.HIDDEN)
    @KaNoContextParameterBridgeRequired
    public fun KaSymbol.deprecationStatus(annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo?

    /**
     * The deprecation status of the given property getter, or `null` if the getter is not deprecated.
     */
    @KaExperimentalApi
    @Deprecated(
        message = "Use 'deprecation' directly instead",
        replaceWith = ReplaceWith("this.getter?.deprecation"),
        level = DeprecationLevel.HIDDEN,
    )
    @KaNoContextParameterBridgeRequired
    public val KaPropertySymbol.getterDeprecationStatus: DeprecationInfo?

    /**
     * The deprecation status of the given property setter, or `null` if the setter is not deprecated or doesn't exist.
     */
    @KaExperimentalApi
    @Deprecated(
        message = "Use 'deprecation' directly instead",
        replaceWith = ReplaceWith("this.setter?.deprecation"),
        level = DeprecationLevel.HIDDEN,
    )
    @KaNoContextParameterBridgeRequired
    public val KaPropertySymbol.setterDeprecationStatus: DeprecationInfo?

    /**
     * A set of applicable targets for an annotation class symbol, or `null` if the symbol is not an annotation class.
     */
    @KaExperimentalApi
    public val KaClassSymbol.annotationApplicableTargets: Set<KotlinTarget>?

    /**
     * Whether the property is an [inline property](https://kotlinlang.org/docs/inline-functions.html#inline-properties).
     * A property is considered `inline` when both of its accessors are `inline` or when it has the `inline` keyword.
     * The `inline` keyword on a property is syntactic sugar for marking both accessors as `inline`.
     */
    @KaExperimentalApi
    public val KaKotlinPropertySymbol.isInline: Boolean

    /**
     * A [FqName] which can be used to import the given symbol, or `null` if the symbol cannot be imported.
     */
    @KaIdeApi
    public val KaSymbol.importableFqName: FqName?

    /**
     * The return value status of the function (should it be used, or can it be ignored).
     * See the [KEEP](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0412-unused-return-value-checker.md) for details.
     */
    @KaExperimentalApi
    @KaK1Unsupported
    public val KaNamedFunctionSymbol.returnValueStatus: KaReturnValueStatus
}

/**
 * Represents the deprecation status of a symbol.
 *
 * @see KaSymbolInformationProvider.deprecation
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDeprecation {
    /** The deprecation level. */
    public val level: Level

    /** Whether this deprecation propagates to overriding members. */
    public val isPropagatedToOverrides: Boolean

    /**
     * Deprecation severity levels, corresponding to [DeprecationLevel] in the Kotlin standard library.
     *
     * The levels form a progression from least to most restrictive: [WARNING] produces a compiler warning, [ERROR] produces a compiler
     * error, and [HIDDEN] makes the declaration invisible to new code.
     */
    @KaExperimentalApi
    public enum class Level {
        /**
         * The deprecated symbol can still be used, but a warning is reported at the call site.
         *
         * Corresponds to [DeprecationLevel.WARNING].
         */
        WARNING,

        /**
         * The deprecated symbol can still be referenced, but a compilation error is reported at the call site.
         *
         * Corresponds to [DeprecationLevel.ERROR].
         */
        ERROR,

        /**
         * The deprecated symbol is no longer visible to new code.
         * Existing compiled code that references it will still work, but new source code cannot reference it.
         *
         * Corresponds to [DeprecationLevel.HIDDEN].
         */
        HIDDEN,
    }
}

/**
 * The return value status of the function (should it be used, or can it be ignored).
 * @see org.jetbrains.kotlin.analysis.api.components.KaSymbolInformationProvider.returnValueStatus
 */
@KaExperimentalApi
@KaK1Unsupported
public sealed class KaReturnValueStatus(public val name: String) {
    override fun toString(): String = name

    /**
     * The return value of the function must be checked for usage.
     */
    @KaExperimentalApi
    public data object MustUse : KaReturnValueStatus("MustUse")

    /**
     * The return value of the function is declared as explicitly ignorable and should not be checked for usage.
     */
    @KaExperimentalApi
    public data object ExplicitlyIgnorable : KaReturnValueStatus("ExplicitlyIgnorable")

    /**
     * The return value status of the function is unspecified.
     */
    @KaExperimentalApi
    public data object Unspecified : KaReturnValueStatus("Unspecified")

    /**
     * A dummy private subclass to force 'else' branches in client code
     */
    @Suppress("unused")
    @KaExperimentalApi
    private data object Unknown : KaReturnValueStatus("Unknown")
}

/**
 * The deprecation status of the given symbol, or `null` if the symbol is not deprecated.
 *
 * This considers deprecation annotations applied to the symbol itself and, for property-related
 * symbols, deprecation annotations with appropriate use-site targets.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val KaSymbol.deprecation: KaDeprecation?
    get() = with(session) { deprecation }

/**
 * Whether the symbol is deprecated at any level ([WARNING][KaDeprecation.Level.WARNING], [ERROR][KaDeprecation.Level.ERROR],
 * or [HIDDEN][KaDeprecation.Level.HIDDEN]).
 *
 * This is a convenience property equivalent to `deprecation != null`.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val KaSymbol.isDeprecated: Boolean
    get() = with(session) { isDeprecated }

/**
 * Whether the function symbol meets all the requirements to be declared as an [operator function](https://kotlinlang.org/docs/operator-overloading.html).
 *
 * In Kotlin, the set of functions which can be declared as an operator is predefined. [canBeOperator] not only checks the name of a
 * potential operator function, but also its signature, depending on the operator.
 *
 * [canBeOperator] does not determine whether the function symbol *is* declared as an operator. For this purpose, use
 * [KaNamedFunctionSymbol.isOperator] instead.
 *
 * #### Example
 *
 * ```kotlin
 * class A
 *
 * fun A.plus(that: A): A = A() // canBeOperator = true, as it meets all requirements for `plus`.
 *
 * operator fun A.contains(that: A): Boolean = true // canBeOperator = true, as it's already an operator.
 *
 * fun A.something(that: A): A = A() // canBeOperator = false, as there is no operator with such a name.
 *
 * fun A.minus(): A = A() // canBeOperator = false, as `minus` is a binary operator and should have one parameter.
 * ```
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val KaNamedFunctionSymbol.canBeOperator: Boolean
    get() = with(session) { canBeOperator }

/**
 * The deprecation status of the given symbol for the given [annotation use-site target][useSiteTarget],
 * or `null` if the symbol is not deprecated for that target.
 *
 * @param useSiteTarget the annotation use-site target to check deprecation for
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KaSymbol.deprecation(useSiteTarget: KaAnnotationUseSiteTarget?): KaDeprecation? {
    return with(session) {
        deprecation(
            useSiteTarget = useSiteTarget,
        )
    }
}

/**
 * A set of applicable targets for an annotation class symbol, or `null` if the symbol is not an annotation class.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val KaClassSymbol.annotationApplicableTargets: Set<KotlinTarget>?
    get() = with(session) { annotationApplicableTargets }

/**
 * Whether the property is an [inline property](https://kotlinlang.org/docs/inline-functions.html#inline-properties).
 * A property is considered `inline` when both of its accessors are `inline` or when it has the `inline` keyword.
 * The `inline` keyword on a property is syntactic sugar for marking both accessors as `inline`.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val KaKotlinPropertySymbol.isInline: Boolean
    get() = with(session) { isInline }

/**
 * A [FqName] which can be used to import the given symbol, or `null` if the symbol cannot be imported.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaIdeApi
@KaContextParameterApi
context(session: KaSession)
public val KaSymbol.importableFqName: FqName?
    get() = with(session) { importableFqName }

/**
 * The return value status of the function (should it be used, or can it be ignored).
 * See the [KEEP](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0412-unused-return-value-checker.md) for details.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaK1Unsupported
@KaContextParameterApi
context(session: KaSession)
public val KaNamedFunctionSymbol.returnValueStatus: KaReturnValueStatus
    get() = with(session) { returnValueStatus }
