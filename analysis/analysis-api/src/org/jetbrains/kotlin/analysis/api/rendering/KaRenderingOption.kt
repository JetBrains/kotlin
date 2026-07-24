/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.scopes.declaredMemberScope
import org.jetbrains.kotlin.analysis.api.scopes.staticDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.augmentedByWarningLevelAnnotations

/**
 * A typed configuration flag that influences rendering. Its [defaultValue] is used unless overridden via [KaRendererBuilder.set].
 *
 * The available options are defined as properties of the companion object.
 */
@KaExperimentalApi
public class KaRenderingOption<T> internal constructor(public val defaultValue: T) {
    @KaExperimentalApi
    public companion object {
        /** How qualified class type names are rendered (with the package, with outer classifiers, or as a simple name). */
        public val ClassTypeQualification: KaRenderingOption<KaClassTypeQualification> =
            KaRenderingOption(KaClassTypeQualification.WITH_OUTER_CLASSIFIERS)

        /**
         * A transformation applied to every rendered [KaType] before it is printed.
         *
         * The transformation is applied to each type as it is rendered, so it also affects nested types, such as type arguments, upper
         * bounds, and the components of flexible and intersection types. It runs before [TypeApproximation].
         *
         * By default, warning-level nullability annotations are treated as strict ones (see [augmentedByWarningLevelAnnotations]), so
         * `@RecentlyNullable X!` is rendered as `X?`.
         */
        public val TypeTransformation: KaRenderingOption<context(KaSession, KaRenderingContext) (KaType) -> KaType> =
            KaRenderingOption { type -> type.augmentedByWarningLevelAnnotations }

        /**
         * Whether every rendered [KaType] is approximated to a type that can be written in Kotlin source code, and in which direction.
         *
         * The approximation is applied to each type as it is rendered, so it also affects nested types. It runs after
         * [TypeTransformation].
         */
        public val TypeApproximation: KaRenderingOption<KaTypeApproximation> =
            KaRenderingOption(KaTypeApproximation.NONE)

        /** Whether the primary constructor is rendered in the class header (e.g. `class Foo(x: Int)`) rather than as a body member. */
        public val PrimaryConstructorInClassHeader: KaRenderingOption<Boolean> =
            KaRenderingOption(true)

        /** Whether the `context(...)` receivers of the given element are placed on their own line rather than inline. */
        public val ContextReceiversOnNewLine: KaRenderingOption<context(KaSession, KaRenderingContext) (Any) -> Boolean> =
            KaRenderingOption { _ -> true }

        /** Whether the annotations of the given element are placed on their own line rather than inline before it. */
        public val AnnotationsOnNewLine: KaRenderingOption<context(KaSession, KaRenderingContext) (KaAnnotated) -> Boolean> =
            KaRenderingOption { value ->
                when (value) {
                    is KaType, is KaTypeParameterSymbol, is KaParameterSymbol, is KaEnumEntrySymbol -> false
                    is KaConstructorSymbol -> !value.isPrimary
                    else -> true
                }
            }

        /** Whether a blank line separates consecutive members within a class body. */
        public val ExtraLineBetweenMembers: KaRenderingOption<Boolean> =
            KaRenderingOption(true)

        /** The symbol origins whose members are excluded from a rendered class body (e.g. inherited, generated, or delegated members). */
        public val ClassMemberOrigins: KaRenderingOption<Set<KaSymbolOrigin>> =
            KaRenderingOption(
                setOf(
                    KaSymbolOrigin.DELEGATED,
                    KaSymbolOrigin.SOURCE_MEMBER_GENERATED,
                    KaSymbolOrigin.SUBSTITUTION_OVERRIDE,
                    KaSymbolOrigin.INTERSECTION_OVERRIDE,
                )
            )

        /** Computes the members to render for a class, in the order they are gathered. */
        public val ClassMembers: KaRenderingOption<context(KaSession, KaRenderingContext) (KaClassSymbol) -> List<KaSymbol>> =
            KaRenderingOption { classSymbol ->
                val context = contextOf<KaRenderingContext>()
                val excludePrimaryConstructor = context.valueFor(PrimaryConstructorInClassHeader)
                val allowedOrigins = context.valueFor(ClassMemberOrigins)

                buildList {
                    val declaredScope = classSymbol.declaredMemberScope

                    if (!classSymbol.classKind.isObject) {
                        addAll(declaredScope.constructors.filter { !excludePrimaryConstructor || !it.isPrimary }.toList())
                    }

                    addAll(declaredScope.callables.toList())

                    val staticDeclaredScope = classSymbol.staticDeclaredMemberScope
                    addAll(staticDeclaredScope.callables.toList())
                    addAll(staticDeclaredScope.classifiers.toList())
                }.filterNot { it.origin in allowedOrigins }
            }

        /** A comparator establishing the order in which class members are rendered. The default (returning `0`) keeps the [ClassMembers] order. */
        public val ClassMemberOrdering: KaRenderingOption<context(KaSession, KaRenderingContext) (KaSymbol, KaSymbol) -> Int> =
            KaRenderingOption { _, _ -> 0 }
    }
}

/** Controls how qualified the name of a class type is rendered. */
@KaExperimentalApi
public enum class KaClassTypeQualification {
    /** The fully qualified name, including the package, e.g. `org.example.Foo.Nested`. */
    FULLY_QUALIFIED,

    /** The name prefixed with its outer classifiers, but without the package, e.g. `Foo.Nested`. */
    WITH_OUTER_CLASSIFIERS,

    /** The simple name only, e.g. `Nested`. */
    SIMPLE,
}

/**
 * Controls whether rendered types are approximated to *denotable* types (types which can be written in Kotlin source code), and in which
 * direction.
 *
 * Approximation matters for types that cannot be written down by a user, such as captured types, intersection types, or types with
 * flexible bounds. Such a type usually has both a denotable subtype and a denotable supertype, and the correct choice depends on the
 * position in which the type is used: an output position (e.g. a return type) requires a supertype, while an input position (e.g. a value
 * parameter type) requires a subtype.
 *
 * @see KaRenderingOption.TypeApproximation
 */
@KaExperimentalApi
public enum class KaTypeApproximation {
    /** Types are rendered as they are, even if the result cannot be written in Kotlin source code. */
    NONE,

    /** Every type is approximated to its closest denotable subtype, which is correct for input positions, such as parameter types. */
    TO_DENOTABLE_SUBTYPE,

    /**
     * Every type is approximated to its closest denotable supertype, which is correct for output positions, such as return types.
     * Locally declared types are approximated to local supertypes.
     */
    TO_DENOTABLE_SUPERTYPE,
}
