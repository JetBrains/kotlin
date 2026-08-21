/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaSpi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaCapturedType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KaDynamicType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionValueParameter
import org.jetbrains.kotlin.analysis.api.types.KaIntersectionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Renders a single [KaPiece] into the contextual [KaRenderingOutput].
 *
 * Renderers for a given [piece] form a stack within a [KaRenderer]; [KaRendererBuilder.push] adds one on top. A renderer may
 * emit nested pieces via the top-level [render] function and fall back to the renderer beneath it by invoking its `next` callback.
 *
 * @param piece the piece this renderer is responsible for.
 */
@KaSpi
@KaExperimentalApi
public abstract class KaPieceRenderer<T>(public val piece: KaPiece<T>) {
    /**
     * Renders [value] into the contextual [KaRenderingOutput]. Returns `true` if the value was handled, or `false` to fall back to
     * the renderer beneath this one. Invoke [next] to render [value] with the renderer immediately beneath this one.
     */
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    public abstract fun render(value: T, next: () -> Unit): Boolean

    @KaExperimentalApi
    public companion object {
        /**
         * The no-op renderer for the given [piece].
         */
        public fun <T> empty(piece: KaPiece<T>): KaPieceRenderer<T> {
            return object : KaPieceRenderer<T>(piece) {
                context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
                override fun render(value: T, next: () -> Unit): Boolean = true
            }
        }
    }
}

/** The rendering state passed to every [KaPieceRenderer], used to render nested pieces and to read [KaRenderingOption] values. */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaRenderingContext {
    /** Renders [value] as the given [piece], dispatching to the corresponding renderer stack. */
    context(session: KaSession)
    public fun <T> render(value: T, piece: KaPiece<T>)

    /**
     * Renders [value] as the given [piece], dispatching to the corresponding renderer stack, but writes the result into [output]
     * instead of the output of the current rendering. Nested pieces are written into [output] as well.
     *
     * This allows a renderer to capture the rendered form of a piece, e.g. to post-process it or to render it as a single fragment.
     */
    context(session: KaSession)
    public fun <T> render(value: T, piece: KaPiece<T>, output: KaRenderingOutput)

    /**
     * Whether the current renderer has been requested by [piece] (possibly transitively).
     *
     * The piece which the current renderer renders is not a requester of itself. Passing that piece therefore detects nesting, such as a
     * [KaPiece.Type] which is rendered as a type argument of another [KaPiece.Type].
     */
    public fun <T> isInside(piece: KaPiece<T>): Boolean

    /** Returns the effective value of [option] for this renderer (the overridden value, or its [KaRenderingOption.defaultValue]). */
    public fun <T> valueFor(option: KaRenderingOption<T>): T
}

/** Renders [value] as the given [piece], dispatching to the corresponding renderer stack. */
@KaExperimentalApi
context(session: KaSession, context: KaRenderingContext)
public fun <T> render(value: T, piece: KaPiece<T>) {
    context.render(value, piece)
}

/**
 * A typed, enumerated key that identifies a specific piece of a symbol or a type to render.
 *
 * Each [KaPiece] is associated with a stack of [KaPieceRenderer]s in a [KaRenderer]. Renderers may render other pieces via
 * [KaRenderingContext], which allows customizing individual parts of the output without reimplementing the whole rendering.
 *
 * The two top-level entry points are [Symbol] (renders any [KaSymbol]) and [Type] (renders any [KaType]).
 */
@KaExperimentalApi
public class KaPiece<T> internal constructor() {
    @KaExperimentalApi
    public companion object {
        /**
         * The annotations of an annotated element, including the whitespace which separates them from what follows.
         *
         * Which annotations are rendered is controlled by [KaRenderingOption.Annotations], and whether each is followed by a line break
         * or a space by [KaRenderingOption.AnnotationsOnNewLine].
         *
         * The second component is the use-site target which the annotations are rendered with, e.g. `@receiver:Foo`, or `null` when they
         * are rendered without one.
         */
        public val Annotations: KaPiece<Pair<KaAnnotated, AnnotationUseSiteTarget?>> = KaPiece()

        /**
         * A single annotation entry, e.g. `@Foo(bar)`. The annotation class name is rendered as [ClassName], respecting
         * [KaRenderingOption.ClassTypeQualification].
         *
         * The second component is the use-site target which the annotation is rendered with, e.g. `@get:Foo(bar)`, or `null` when it is
         * rendered without one.
         */
        public val Annotation: KaPiece<Pair<KaAnnotation, AnnotationUseSiteTarget?>> = KaPiece()

        /**
         * The parenthesized argument list of an annotation, e.g. `(message = "Use bar")`. Nothing is rendered for an annotation without
         * arguments.
         */
        public val AnnotationValues: KaPiece<KaAnnotation> = KaPiece()

        /** A single annotation argument value (a constant, enum entry, class literal, nested annotation, or array). */
        public val AnnotationValue: KaPiece<KaAnnotationValue> = KaPiece()

        /** A constant value, e.g. `42`, `"text"`, or `null`. */
        public val ConstantValue: KaPiece<KaConstantValue> = KaPiece()

        /**
         * Any [KaType]. Applies [KaRenderingOption.TypeTransformation], [KaRenderingOption.TypeApproximation], and
         * [KaRenderingOption.ClassTypeRenderingMode] to the type (in the mentioned order), then dispatches to the piece of its kind.
         */
        public val Type: KaPiece<KaType> = KaPiece()

        /** The annotations attached to a [KaType], rendered as [Annotations] (each followed by a space by default). */
        public val TypeAnnotations: KaPiece<KaType> = KaPiece()

        /** The nullability marker of a type, e.g. `?` in `String?`. Nothing is rendered for a non-nullable type or a flexible type. */
        public val TypeNullability: KaPiece<KaType> = KaPiece()

        /** A type projection: an explicit type argument with its variance, e.g. `Number`, `out Number`, or a star projection `*`. */
        public val TypeProjection: KaPiece<KaTypeProjection> = KaPiece()

        /** A comma-separated list of type arguments enclosed in angle brackets, e.g. `<Int, String>`. */
        public val TypeArgumentList: KaPiece<List<KaTypeProjection>> = KaPiece()

        /** A class type, e.g. `List<String>?`, comprising its [TypeAnnotations], [ClassTypeName], and [TypeNullability]. */
        public val ClassType: KaPiece<KaClassType> = KaPiece()

        /**
         * The name of a class type, e.g. `Foo.Bar` or `Int` for `Foo.Bar<Int>`. How the qualified name is rendered is controlled by
         * [KaRenderingOption.ClassTypeQualification].
         */
        public val ClassTypeName: KaPiece<KaClassType> = KaPiece()

        /**
         * A function type rendered with function type syntax, e.g. `(Int) -> String`. A nullable or annotated function type is wrapped in
         * parentheses, e.g. `((Int) -> String)?`.
         *
         * By default, a [reflection function type][KaFunctionType.isReflectType], such as `KFunction1<Int, String>`, is rendered as a
         * [ClassType] as there is no type syntax for reflection types.
         */
        public val FunctionType: KaPiece<KaFunctionType> = KaPiece()

        /**
         * A single value parameter of a [FunctionType], e.g. `x: Int` in `(x: Int) -> String`, or just `Int` for a parameter which has no
         * name.
         *
         * A function type parameter is named by a synthetic `@ParameterName` annotation on its type, so a parameter only has a name when
         * that annotation is present.
         */
        public val FunctionTypeParameter: KaPiece<KaFunctionValueParameter> = KaPiece()

        /** A single context parameter of a [FunctionType], e.g. `A` in `context(A) () -> Unit`. */
        public val FunctionTypeContextParameter: KaPiece<KaContextReceiver> = KaPiece()

        /** A reference to a type parameter, e.g. `T`. */
        public val TypeParameterType: KaPiece<KaTypeParameterType> = KaPiece()

        /** A captured type produced by capturing a projection during type inference, rendered as `Captured(out Number)`. */
        public val CapturedType: KaPiece<KaCapturedType> = KaPiece()

        /** A definitely non-nullable type, e.g. `T & Any`. */
        public val DefinitelyNotNullType: KaPiece<KaDefinitelyNotNullType> = KaPiece()

        /**
         * A flexible type spanning a lower and an upper bound, such as a platform type. With [KaRenderingOption.FlexibleTypeShrinking]
         * (the default), it is rendered compactly when possible, e.g. `String!`; otherwise as the `(lower..upper)` range of its bounds.
         */
        public val FlexibleType: KaPiece<KaFlexibleType> = KaPiece()

        /** An intersection of several types, e.g. `A & B`. */
        public val IntersectionType: KaPiece<KaIntersectionType> = KaPiece()

        /** The Kotlin/JS `dynamic` type. */
        public val DynamicType: KaPiece<KaDynamicType> = KaPiece()

        /**
         * A type that could not be resolved. An unresolved class type is rendered in its written form, e.g. `C<String>`; other error
         * types are rendered as an error marker.
         */
        public val ErrorType: KaPiece<KaErrorType> = KaPiece()

        /** Any [KaSymbol]. Dispatches to a more specific symbol piece depending on the symbol kind. */
        public val Symbol: KaPiece<KaSymbol> = KaPiece()

        /** The name of a declaration, as rendered in the declaration itself, e.g. `foo` in `fun foo()`. */
        public val SymbolName: KaPiece<KaSymbol> = KaPiece()

        /**
         * All modifiers of a declaration in the canonical Kotlin order, each followed by a space, e.g. `private abstract `. The modifier
         * list can be transformed with [KaRenderingOption.Modifiers], and single modifiers filtered out with
         * [KaRenderingOption.AllowedKeywords].
         */
        public val SymbolModifiers: KaPiece<KaDeclarationSymbol> = KaPiece()

        /** A comma-separated list of type parameters enclosed in angle brackets, e.g. `<T : Number>`. */
        public val TypeParameterList: KaPiece<List<KaTypeParameterSymbol>> = KaPiece()

        /** The type parameter list of a callable. */
        public val CallableTypeParameterList: KaPiece<KaCallableSymbol> = KaPiece()

        /** The type parameter list of a class or a type alias. */
        public val ClassifierTypeParameterList: KaPiece<KaClassLikeSymbol> = KaPiece()

        /**
         * A single type parameter, including its `reified` modifier, variance, and a single upper bound, e.g. `in T : Number`. Multiple
         * upper bounds are rendered in a [WhereClause] instead.
         */
        public val TypeParameter: KaPiece<KaTypeParameterSymbol> = KaPiece()

        /**
         * The `where` clause listing the bounds of type parameters that have more than one upper bound, including the leading space,
         * e.g. ` where T : A, T : B`. Nothing is rendered when no type parameter has multiple bounds.
         */
        public val WhereClause: KaPiece<List<KaTypeParameterSymbol>> = KaPiece()

        /** The `where` clause of a callable. */
        public val CallableWhereClause: KaPiece<KaCallableSymbol> = KaPiece()

        /** The `where` clause of a class or a type alias. */
        public val ClassifierWhereClause: KaPiece<KaClassLikeSymbol> = KaPiece()

        /** A `context(...)` clause, followed by a line break or a space (see [KaRenderingOption.ContextReceiversOnNewLine]). */
        public val ContextParameterList: KaPiece<List<KaContextParameterSymbol>> = KaPiece()

        /** The `context(...)` clause of a callable. Nothing is rendered when there are no context parameters. */
        public val CallableContextParameterList: KaPiece<KaCallableSymbol> = KaPiece()

        /** A single context parameter. */
        public val ContextParameter: KaPiece<KaContextParameterSymbol> = KaPiece()

        /** The annotations of a context parameter. */
        public val ContextParameterAnnotations: KaPiece<KaContextParameterSymbol> = KaPiece()

        /** The type of context parameter, including the leading colon, e.g. `: String`. */
        public val ContextParameterType: KaPiece<KaContextParameterSymbol> = KaPiece()

        /** The parenthesized value parameter list, e.g. `(a: Int, b: String)`. */
        public val ValueParameterList: KaPiece<List<KaValueParameterSymbol>> = KaPiece()

        /**
         * A single value parameter, including its modifiers, name, type, and default value. A parameter which declares a property in the
         * primary constructor also carries the property's modifiers and its `val`/`var` (see
         * [KaRenderingOption.PrimaryConstructorInClassHeader]).
         */
        public val ValueParameter: KaPiece<KaValueParameterSymbol> = KaPiece()

        /**
         * The annotations of a value parameter. For a parameter which declares a property in the primary constructor, the annotations of
         * the property and its components are also rendered here, with use-site targets, e.g. `@property:Foo`.
         */
        public val ValueParameterAnnotations: KaPiece<KaValueParameterSymbol> = KaPiece()

        /** The type of value parameter, including the leading colon, e.g. `: String`. */
        public val ValueParameterType: KaPiece<KaValueParameterSymbol> = KaPiece()

        /**
         * The default value of a value parameter, including the leading `=`, e.g. `= ...`. Nothing is rendered for a parameter without
         * a default value.
         */
        public val ValueParameterDefaultValue: KaPiece<KaValueParameterSymbol> = KaPiece()

        /**
         * The default value *expression* of a value parameter, without the leading `=`.
         *
         * The expression is not part of the symbol, so it is rendered as an `...` placeholder.
         */
        public val ValueParameterDefaultValueExpression: KaPiece<KaValueParameterSymbol> = KaPiece()

        /** Any [KaFunctionSymbol]; used for anonymous functions and SAM constructors. */
        public val Function: KaPiece<KaFunctionSymbol> = KaPiece()

        /** The annotations of a function. */
        public val FunctionAnnotations: KaPiece<KaFunctionSymbol> = KaPiece()

        /**
         * The receiver of an extension callable, including the trailing dot, e.g. `String.` in `fun String.foo()`. A function type or
         * definitely non-nullable receiver is parenthesized, e.g. `(() -> Unit).`.
         */
        public val FunctionReceiver: KaPiece<KaParameterSymbol> = KaPiece()

        /** The value parameter list of a function. */
        public val FunctionValueParameterList: KaPiece<KaFunctionSymbol> = KaPiece()

        /** The return type of function, including the leading colon, e.g. `: String`. The type is always rendered. */
        public val FunctionReturnType: KaPiece<KaFunctionSymbol> = KaPiece()

        /**
         * The return type of named function, including the leading colon, e.g. `: String`. Nothing is rendered for an implicit `Unit`
         * return type.
         */
        public val NamedFunctionReturnType: KaPiece<KaNamedFunctionSymbol> = KaPiece()

        /**
         * The body of a function, including whatever separates it from the signature, e.g. ` { ... }` or ` = 42`. The body is not part of
         * the symbol, so nothing is rendered by default.
         *
         * The piece covers named functions, anonymous functions, SAM constructors, and property accessors. The body of a constructor is
         * rendered as [ConstructorBody] instead.
         */
        public val FunctionBody: KaPiece<KaFunctionSymbol> = KaPiece()

        /** A named function declaration. */
        public val NamedFunction: KaPiece<KaNamedFunctionSymbol> = KaPiece()

        /** A constructor declaration. */
        public val Constructor: KaPiece<KaConstructorSymbol> = KaPiece()

        /**
         * The body of a constructor, including whatever separates it from the signature, e.g. ` { ... }`. The body is not part of the
         * symbol, so nothing is rendered by default.
         *
         * The piece is not rendered for [PrimaryConstructor]s as they do not have a body.
         */
        public val ConstructorBody: KaPiece<KaConstructorSymbol> = KaPiece()

        /**
         * The primary constructor of a class, as rendered in the class header, e.g. `(x: Int)` in `class Foo(x: Int)` (see
         * [KaRenderingOption.PrimaryConstructorInClassHeader]).
         *
         * A primary constructor with annotations or an explicit visibility is rendered with a leading space and the `constructor`
         * keyword, as in `class Foo private constructor(x: Int)`. Without parameters, the parentheses are omitted.
         */
        public val PrimaryConstructor: KaPiece<KaConstructorSymbol> = KaPiece()

        /**
         * The accessors of a property which are rendered below it, each on its own indented line: the non-default accessors, and the
         * default accessors carrying annotations.
         */
        public val PropertyAccessors: KaPiece<KaPropertySymbol> = KaPiece()

        /** A property getter or setter. */
        public val PropertyAccessor: KaPiece<KaPropertyAccessorSymbol> = KaPiece()

        /** A property declaration (`val` or `var`). */
        public val Property: KaPiece<KaPropertySymbol> = KaPiece()

        /**
         * The annotations of a property, including those of its backing field and default setter parameter, which have no rendered
         * declaration of their own and so are rendered here with use-site targets, e.g. `@field:Foo`.
         */
        public val PropertyAnnotations: KaPiece<KaPropertySymbol> = KaPiece()

        /** The type of property, including the leading colon, e.g. `: String`. */
        public val PropertyReturnType: KaPiece<KaPropertySymbol> = KaPiece()

        /**
         * The initializer of a property, including the leading `=`, e.g. `= 42`.
         *
         * Only the initializer of a `const` property is rendered by default, as it is a compile-time constant which is part of the
         * property's declaration in source code.
         */
        public val PropertyInitializer: KaPiece<KaPropertySymbol> = KaPiece()

        /** A local variable declaration. */
        public val LocalVariable: KaPiece<KaLocalVariableSymbol> = KaPiece()

        /** A field declared in Java. */
        public val JavaField: KaPiece<KaJavaFieldSymbol> = KaPiece()

        /** The declaration of a property's backing field, e.g. `field: MutableList<String>`. */
        public val BackingField: KaPiece<KaBackingFieldSymbol> = KaPiece()

        /** A single enum entry, including its anonymous object body when present, e.g. `ENTRY { ... }`. */
        public val EnumEntry: KaPiece<KaEnumEntrySymbol> = KaPiece()

        /**
         * A named class, interface, or object declaration. The primary constructor is rendered in the class header when
         * [KaRenderingOption.PrimaryConstructorInClassHeader] is set (the default).
         */
        public val NamedClass: KaPiece<KaNamedClassSymbol> = KaPiece()

        /** An anonymous object, e.g. `object : Runnable { ... }`. */
        public val AnonymousObject: KaPiece<KaAnonymousObjectSymbol> = KaPiece()

        /** A type alias declaration. */
        public val TypeAlias: KaPiece<KaTypeAliasSymbol> = KaPiece()

        /** The annotations of a class. */
        public val ClassAnnotations: KaPiece<KaNamedClassSymbol> = KaPiece()

        /**
         * The supertype list of a class, including the leading colon, e.g. ` : Base(), Runnable`. Nothing is rendered when there are no
         * non-trivial supertypes (`Any` and the implicit `Enum`/`Annotation` supertypes are omitted).
         */
        public val SupertypeList: KaPiece<KaClassSymbol> = KaPiece()

        /** A single supertype, including the constructor call parentheses of a class supertype, e.g. `Base()`. */
        public val Supertype: KaPiece<KaType> = KaPiece()

        /**
         * The body of a class, containing its member declarations, including the braces and the leading space, e.g. ` { ... }`. Nothing
         * is rendered for an empty body. The rendered members and their layout are controlled by [KaRenderingOption.ClassMembers],
         * [KaRenderingOption.ClassMemberOrdering], and [KaRenderingOption.ExtraLineBetweenMembers].
         */
        public val ClassBody: KaPiece<KaClassSymbol> = KaPiece()

        /** A package, e.g. `package kotlin.collections`. */
        public val Package: KaPiece<KaPackageSymbol> = KaPiece()

        /**
         * The fully-qualified name of a package, rendered segment by segment, e.g. `org.example`.
         *
         * Each segment is linked to the [KaPackageSymbol] of the package it forms, when [KaRenderingOption.LinkSymbols] is enabled and
         * that package can be resolved.
         *
         * The piece renders the segments alone. A dot which separates the package name from what follows, such as the dot in a fully
         * qualified class name, is rendered by the piece that requested it.
         */
        public val PackageName: KaPiece<FqName> = KaPiece()

        /**
         * The qualified name of a class, rendered segment by segment, e.g. `Foo.Bar`. How qualified the name is rendered is controlled
         * by [KaRenderingOption.ClassTypeQualification]. Each segment is linked to the classifier it names, when
         * [KaRenderingOption.LinkSymbols] is enabled and it can be resolved.
         *
         * The piece is used in positions which name a class without type arguments, such as annotations. The name of a class *type*,
         * which includes the type arguments of each rendered qualifier segment, is [ClassTypeName].
         */
        public val ClassName: KaPiece<ClassId> = KaPiece()
    }
}
