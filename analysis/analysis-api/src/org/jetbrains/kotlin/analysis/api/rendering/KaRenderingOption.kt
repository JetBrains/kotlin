/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.scopes.declaredMemberScope
import org.jetbrains.kotlin.analysis.api.scopes.staticDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.augmentedByWarningLevelAnnotations
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens

/**
 * A typed configuration flag that influences rendering. Its [defaultValue] is used unless overridden via [KaRendererBuilder.set].
 *
 * The available options are defined as properties of the companion object.
 */
@KaExperimentalApi
public class KaRenderingOption<T> private constructor(public val defaultValue: T) {
    /**
     * Replaces the modifier list for the given [KaDeclarationSymbol].
     *
     * @see [Modifiers]
     */
    public typealias ModifierListTransformer =
            context(KaSession, KaRenderingContext) (KaDeclarationSymbol, List<KtModifierKeywordToken>) -> List<KtModifierKeywordToken>

    /**
     * Transforms the given [KaType] into another, which is then used for rendering.
     *
     * @see [TypeTransformation]
     */
    public typealias TypeTransformer =
            context(KaSession, KaRenderingContext) (KaType) -> KaType

    /**
     * Returns the list of rendered [KaAnnotation]s for the given [KaAnnotated] entity.
     */
    public typealias AnnotationProvider =
            context(KaSession, KaRenderingContext) (KaAnnotated) -> List<KaAnnotation>

    /**
     * Returns the list of rendered member [KaSymbol]s for the given [KaClassSymbol].
     */
    public typealias ClassMemberProvider =
            context(KaSession, KaRenderingContext) (KaClassSymbol) -> List<KaSymbol>

    /**
     * Orders [KaSymbol] members inside a class.
     * Returns:
     * - `-1` if the first symbol should be rendered before the second one;
     * - `1` if the first symbol should be rendered after the next one;
     * - `0` if member order should be kept intact.
     */
    public typealias ClassMemberComparator =
            context(KaSession, KaRenderingContext) (KaSymbol, KaSymbol) -> Int

    @KaExperimentalApi
    public companion object {
        /**
         * A function specifying which keywords are rendered.
         * A keyword for which the predicate returns `false` is omitted, together with the whitespace which separates it from what follows.
         *
         * The option covers the keywords of declarations and types, such as modifiers, `fun`, `class`, `constructor`, `where`, and
         * `dynamic`. It does not affect keywords which are part of a value, such as `null`, `true`, and `false`.
         *
         * By default, all keywords are rendered.
         */
        public val AllowedKeywords: KaRenderingOption<(KtKeywordToken) -> Boolean> =
            KaRenderingOption { _ -> true }

        /**
         * Transforms the modifiers of a declaration. The [ModifierListTransformer] may drop, add, or reorder modifiers.
         *
         * The default modifiers are given in the canonical Kotlin order (see [KtTokens.MODIFIER_KEYWORDS_ARRAY]), and the returned
         * modifiers are rendered in the order they are returned. As modifiers are keywords, [AllowedKeywords] is still applied to each of
         * them afterward.
         *
         * The option covers every modifier of every declaration, including the modifiers of value parameters (`vararg`, `crossinline`,
         * `noinline`) and of type parameters (`reified`, and the declaration-site variance `in`/`out`). It does not cover keywords which
         * are not modifiers, such as `fun`, `val`, `var`, `class`, or `constructor`.
         *
         * By default, the declaration's own modifiers are rendered in the canonical order.
         */
        public val Modifiers: KaRenderingOption<ModifierListTransformer> = KaRenderingOption { _, modifiers -> modifiers }

        /**
         * Whether rendered identifiers are linked to the symbols they reference via [KaTextAttribute.Symbol].
         *
         * When disabled, renderers also skip the symbol resolution which is only needed for linking, such as resolving the segments of
         * qualified class and package names. Disabled by default, as an output which produces plain text drops the links anyway.
         */
        public val LinkSymbols: KaRenderingOption<Boolean> = KaRenderingOption(false)

        /** How qualified class type names are rendered (with the package, with outer classifiers, or as a simple name). */
        public val ClassTypeQualification: KaRenderingOption<KaClassTypeQualification> =
            KaRenderingOption(KaClassTypeQualification.WITH_OUTER_CLASSIFIERS)

        /**
         * How a class type which involves a type alias is rendered: as the abbreviation (the type alias application), as its expansion, or
         * as one of them with the other in a comment.
         *
         * The mode is applied to each type as it is rendered, so it also affects nested types, such as type arguments. It runs after
         * [TypeTransformation] and [TypeApproximation].
         */
        public val ClassTypeRenderingMode: KaRenderingOption<KaClassTypeRenderingMode> =
            KaRenderingOption(KaClassTypeRenderingMode.ABBREVIATION)

        /**
         * A transformation applied to every rendered [KaType] before it is printed.
         *
         * The transformation is applied to each type as it is rendered, so it also affects nested types, such as type arguments, upper
         * bounds, and the components of flexible and intersection types. It runs before [TypeApproximation].
         *
         * By default, warning-level nullability annotations are treated as strict ones (see [augmentedByWarningLevelAnnotations]), so
         * `@RecentlyNullable X!` is rendered as `X?`.
         */
        public val TypeTransformation: KaRenderingOption<TypeTransformer> =
            KaRenderingOption { type -> type.augmentedByWarningLevelAnnotations }

        /**
         * Whether every rendered [KaType] is approximated to a type that can be written in Kotlin source code, and in which direction.
         *
         * The approximation is applied to each type as it is rendered, so it also affects nested types.
         * It runs after [TypeTransformation].
         */
        public val TypeApproximation: KaRenderingOption<KaTypeApproximation> =
            KaRenderingOption(KaTypeApproximation.NONE)

        /**
         * Whether a flexible type is shrunk to its compact form instead of being rendered as the `(lower..upper)` range of its bounds.
         *
         * A flexible type whose bounds differ only in nullability is shrunk to its lower bound followed by `!`, so `String..String?` is
         * rendered as `String!`. A flexible type between a mutable collection and its read-only counterpart is shrunk to the collection
         * name carrying a `(Mutable)` marker, so `MutableList<String!>..List<String!>?` is rendered as `(Mutable)List<String!>!`. Any
         * other flexible type has no compact form and is rendered as a range regardless of this option.
         *
         * The option is applied to each type as it is rendered, so it also affects nested types, such as type arguments.
         *
         * Flexible types are shrunk by default, as the range form exposes the bounds of a type which cannot be written in Kotlin source
         * code in the first place.
         */
        public val FlexibleTypeShrinking: KaRenderingOption<Boolean> =
            KaRenderingOption(true)

        /** Whether the primary constructor is rendered in the class header (e.g. `class Foo(x: Int)`) rather than as a body member. */
        public val PrimaryConstructorInClassHeader: KaRenderingOption<Boolean> =
            KaRenderingOption(true)

        /** Whether the `context(...)` receivers of the given element are placed on their own line rather than inline. */
        public val ContextReceiversOnNewLine: KaRenderingOption<context(KaSession, KaRenderingContext) (Any) -> Boolean> =
            KaRenderingOption { _ -> true }

        /**
         * Computes the annotations to render for an annotated element, in the order they are rendered. An omitted annotation is dropped
         * together with the whitespace which separates it from what follows.
         *
         * By default, all annotations of the element are rendered in their original order, except for `@ParameterName`. That annotation is
         * how the name of a function type parameter is encoded, and the name is already rendered as part of the parameter itself (see
         * [KaPiece.FunctionTypeParameter]), so rendering the annotation as well would duplicate it.
         */
        public val Annotations: KaRenderingOption<AnnotationProvider> =
            KaRenderingOption { value ->
                value.annotations.filter { it.classId != StandardNames.FqNames.parameterNameClassId }
            }

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

        /**
         * Computes the members to render for a [KaClassSymbol], in the order they are gathered.
         *
         * By default, all declared members of a class, interface or an object are rendered, including both static and non-static ones.
         *
         * The primary constructor and the properties which it declares are excluded when the primary constructor is rendered in the class
         * header (see [PrimaryConstructorInClassHeader]), as they are already rendered there.
         */
        public val ClassMembers: KaRenderingOption<ClassMemberProvider> =
            KaRenderingOption { classSymbol ->
                val context = contextOf<KaRenderingContext>()
                val primaryConstructorInClassHeader = context.valueFor(PrimaryConstructorInClassHeader)
                val allowedOrigins = context.valueFor(ClassMemberOrigins)

                buildList {
                    val declaredScope = classSymbol.declaredMemberScope

                    if (!classSymbol.classKind.isObject) {
                        addAll(declaredScope.constructors.filter { !primaryConstructorInClassHeader || !it.isPrimary }.toList())
                    }

                    addAll(declaredScope.callables.toList())

                    val staticDeclaredScope = classSymbol.staticDeclaredMemberScope
                    addAll(staticDeclaredScope.callables.toList())
                    addAll(staticDeclaredScope.classifiers.toList())
                }.filterNot { it.origin in allowedOrigins }
                    .filterNot {
                        // A property declared in the primary constructor is rendered as part of the constructor's value parameter.
                        primaryConstructorInClassHeader && it is KaKotlinPropertySymbol && it.primaryConstructorParameter != null
                    }
            }

        /** A comparator establishing the order in which class members are rendered. */
        public val ClassMemberOrdering: KaRenderingOption<ClassMemberComparator> =
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
 * Controls how a class type which involves a type alias is rendered: either as the *abbreviation* (the type alias application, e.g.
 * `foo.bar.StringAlias`) or as its *expansion* (e.g. `kotlin.String`), optionally with the other one in a comment.
 *
 * The mode applies both to a type which carries an [abbreviation][KaType.abbreviation] and to an unexpanded type alias application (a class
 * type whose symbol is a [KaTypeAliasSymbol]). All modes render the same output for any other type.
 *
 * @see KaRenderingOption.ClassTypeRenderingMode
 */
@KaExperimentalApi
public enum class KaClassTypeRenderingMode {
    /** Only the abbreviation, e.g. `foo.bar.StringAlias`. */
    ABBREVIATION,

    /** The abbreviation, followed by its expansion in a comment, e.g. `foo.bar.StringAlias /* = kotlin.String */`. */
    ABBREVIATION_WITH_EXPANSION_COMMENT,

    /** Only the expansion, e.g. `kotlin.String`. */
    EXPANSION,

    /** The expansion, followed by the abbreviation in a comment, e.g. `kotlin.String /* from: foo.bar.StringAlias */`. */
    EXPANSION_WITH_ABBREVIATION_COMMENT,
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
