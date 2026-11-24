/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationTarget
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.FqName

@Deprecated(
    message = "Session components are an implementation detail of the legacy 'KaSession' API. Use the context-parameter API instead.",
    level = DeprecationLevel.WARNING,
)
@KaExperimentalApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaSymbolInformationProvider : KaLifetimeOwner {
    /**
     * The deprecation status of the given symbol, or `null` if the symbol is not deprecated.
     *
     * This considers deprecation annotations applied to the symbol itself and, for property-related
     * symbols, deprecation annotations with appropriate use-site targets.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.deprecation' from the context-parameter API instead.",
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public val KaSymbol.deprecation: KaDeprecation?

    /**
     * Whether the symbol is deprecated at any level.
     *
     * This is a convenience property equivalent to `deprecation != null`.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.isDeprecated' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.isDeprecated",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.isDeprecated",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public val KaSymbol.isDeprecated: Boolean

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
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.canBeOperator' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.canBeOperator",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.canBeOperator",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public val KaNamedFunctionSymbol.canBeOperator: Boolean

    /**
     * A set of applicable targets for an annotation class symbol, or `null` if the symbol is not an annotation class.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.applicableAnnotationTargets' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.applicableAnnotationTargets",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.applicableAnnotationTargets",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public val KaClassSymbol.applicableAnnotationTargets: Set<KaAnnotationTarget>?

    /**
     * Whether the property is an [inline property](https://kotlinlang.org/docs/inline-functions.html#inline-properties).
     * A property is considered `inline` when both of its accessors are `inline` or when it has the `inline` keyword.
     * The `inline` keyword on a property is syntactic sugar for marking both accessors as `inline`.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.isInline' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.isInline",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.isInline",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public val KaKotlinPropertySymbol.isInline: Boolean

    /**
     * A [FqName] which can be used to import the given symbol, or `null` if the symbol cannot be imported.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.importableFqName' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.importableFqName",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.importableFqName",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaIdeApi
    public val KaSymbol.importableFqName: FqName?

    /**
     * A set of annotation targets matching for the given symbol, or `null` if the symbol cannot be annotated.
     * Annotations with one of targets from the returned set can be placed on the [KaSymbol] without an explicit use-site target.
     *
     * Such as, annotations with [KaAnnotationTarget.PROPERTY] or [KaAnnotationTarget.VALUE_PARAMETER] can be directly placed on
     * a 'val' value parameter, while annotations for [KaAnnotationTarget.PROPERTY_GETTER] and [KaAnnotationTarget.PROPERTY_SETTER]
     * can only be applied using the `@set:AnnotationName` syntax.
     *
     * Check the [Annotation use-site targets](https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets) documentation
     * for additional information.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.defaultAnnotationTargets' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.defaultAnnotationTargets",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.defaultAnnotationTargets",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaIdeApi
    @KaExperimentalApi
    public val KaSymbol.defaultAnnotationTargets: Set<KaAnnotationTarget>?

    /**
     * The return value status of the function (should it be used, or can it be ignored).
     * See the [KEEP](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0412-unused-return-value-checker.md) for details.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.returnValueStatus' from the context-parameter API instead.",
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public val KaNamedFunctionSymbol.returnValueStatus: KaReturnValueStatus

    /**
     * File-level annotations (`@file:SomeAnnotation`) of the source file this top-level [KaDeclarationSymbol] was defined in,
     * or `null` for a nested declaration.
     *
     * This API is only intended to be used by the TypeScript export utility.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.containingFileAnnotations' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.containingFileAnnotations",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.containingFileAnnotations",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaNonPublicApi
    public val KaDeclarationSymbol.containingFileAnnotations: KaAnnotationList?
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.symbols.KaDeprecation] instead.**
 *
 * Represents the deprecation status of a symbol.
 *
 * @see KaSymbolInformationProvider.deprecation
 */
@Deprecated(
    message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.KaDeprecation' instead.",
    replaceWith = ReplaceWith(
        expression = "KaDeprecation",
        imports = [
            "org.jetbrains.kotlin.analysis.api.symbols.KaDeprecation",
        ]
    ),
    level = DeprecationLevel.WARNING,
)
@KaObsoleteComponentApi
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDeprecation {
    /** The deprecation level. */
    public val level: KaDeprecationLevel

    /** Whether this deprecation propagates to overriding members. */
    public val isPropagatedToOverrides: Boolean
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.symbols.KaDeprecationLevel] instead.**
 *
 * Deprecation severity levels, corresponding to [DeprecationLevel] in the Kotlin standard library.
 *
 * The levels form a progression from least to most restrictive: [WARNING] produces a compiler warning, [ERROR] produces a compiler
 * error, and [HIDDEN] makes the declaration invisible to new code.
 */
@Deprecated(
    message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.KaDeprecationLevel' instead.",
    replaceWith = ReplaceWith(
        expression = "KaDeprecationLevel",
        imports = [
            "org.jetbrains.kotlin.analysis.api.symbols.KaDeprecationLevel",
        ]
    ),
    level = DeprecationLevel.WARNING,
)
@KaObsoleteComponentApi
@KaExperimentalApi
public class KaDeprecationLevel private constructor(public val name: String) {
    @KaExperimentalApi
    public companion object {
        /**
         * The deprecated symbol can still be used, but a warning is reported at the call site.
         *
         * Corresponds to [DeprecationLevel.WARNING].
         */
        @JvmField
        public val WARNING: KaDeprecationLevel = KaDeprecationLevel("WARNING")

        /**
         * The deprecated symbol can still be referenced, but a compilation error is reported at the call site.
         *
         * Corresponds to [DeprecationLevel.ERROR].
         */
        @JvmField
        public val ERROR: KaDeprecationLevel = KaDeprecationLevel("ERROR")

        /**
         * The deprecated symbol is no longer visible to new code.
         * Existing compiled code that references it will still work, but new source code cannot reference it.
         *
         * Corresponds to [DeprecationLevel.HIDDEN].
         */
        @JvmField
        public val HIDDEN: KaDeprecationLevel = KaDeprecationLevel("HIDDEN")
    }

    override fun toString(): String = name
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.symbols.KaReturnValueStatus] instead.**
 *
 * The return value status of the function (should it be used, or can it be ignored).
 * @see KaSymbolInformationProvider.returnValueStatus
 */
@Deprecated(
    message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.KaReturnValueStatus' instead.",
    replaceWith = ReplaceWith(
        expression = "KaReturnValueStatus",
        imports = [
            "org.jetbrains.kotlin.analysis.api.symbols.KaReturnValueStatus",
        ]
    ),
    level = DeprecationLevel.WARNING,
)
@KaObsoleteComponentApi
@KaExperimentalApi
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
