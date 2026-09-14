/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * A typed, enumerated key that identifies a specific piece of a symbol or a type to render.
 *
 * Each [KaPiece] is associated with a stack of [KaPieceRenderer]s in a [KaRenderer]. Renderers may render other pieces via
 * [KaRenderingContext], which allows customizing individual parts of the output without reimplementing the whole rendering.
 *
 * The two top-level entry points are [Symbol] (renders any [org.jetbrains.kotlin.analysis.api.symbols.KaSymbol]) and [Type]
 * (renders any [org.jetbrains.kotlin.analysis.api.types.KaType]).
 */
@KaExperimentalApi
public class KaPiece<T> private constructor() {
    @KaExperimentalApi
    public companion object {
        /**
         * Creates a new [KaPiece].
         *
         * Important: the Analysis API provides default [KaPieceRenderer]s only for [KaPiece]s listed in this object.
         * For all custom pieces, you *must* provide the default renderer by yourself.
         * If the renderer is not set for a custom piece, it will be dispatched to [Unknown].
         */
        public fun <T> create(): KaPiece<T> {
            return KaPiece()
        }

        /**
         * A dispatcher for [KaPiece]s for which a renderer is not provided.
         * As such a situation should never happen in correct code, the default implementation of [Unknown] posts
         * an [com.intellij.openapi.diagnostic.Logger.error].
         */
        public val Unknown: KaPiece<Pair<Any?, KaPiece<*>>> = create()

        /**
         * The annotations of an annotated element, including the whitespace which separates them from what follows.
         *
         * Which annotations are rendered is controlled by [KaRenderingOption.Annotations], and whether each is followed by a line break
         * or a space by [KaRenderingOption.AnnotationsOnNewLine].
         *
         * The second component is the use-site target which the annotations are rendered with, e.g. `@receiver:Foo`, or `null` when they
         * are rendered without one.
         */
        public val Annotations: KaPiece<Pair<KaAnnotated, AnnotationUseSiteTarget?>> = create()

        /**
         * A single annotation entry, e.g. `@Foo(bar)`. The annotation class name is rendered as [ClassName], respecting
         * [KaRenderingOption.ClassTypeQualification].
         *
         * The second component is the use-site target which the annotation is rendered with, e.g. `@get:Foo(bar)`, or `null` when it is
         * rendered without one.
         */
        public val Annotation: KaPiece<Pair<KaAnnotation, AnnotationUseSiteTarget?>> = create()

        /**
         * The parenthesized argument list of an annotation, e.g. `(message = "Use bar")`. Nothing is rendered for an annotation without
         * arguments.
         */
        public val AnnotationValues: KaPiece<KaAnnotation> = create()

        /** A single annotation argument value (a constant, enum entry, class literal, nested annotation, or array). */
        public val AnnotationValue: KaPiece<KaAnnotationValue> = create()

        /** A constant value, e.g. `42`, `"text"`, or `null`. */
        public val ConstantValue: KaPiece<KaConstantValue> = create()

        /**
         * Any [org.jetbrains.kotlin.analysis.api.types.KaType]. Applies [KaRenderingOption.TypeTransformation],
         * [KaRenderingOption.TypeApproximation], and [KaRenderingOption.ClassTypeRenderingMode] to the type (in the mentioned order),
         * then dispatches to the piece of its kind.
         */
        public val Type: KaPiece<KaType> = create()

        /** The annotations attached to a [KaType], rendered as [Annotations] (each followed by a space by default). */
        public val TypeAnnotations: KaPiece<KaType> = create()

        /** The nullability marker of a type, e.g. `?` in `String?`. Nothing is rendered for a non-nullable type or a flexible type. */
        public val TypeNullability: KaPiece<KaType> = create()

        /** A type projection: an explicit type argument with its variance, e.g. `Number`, `out Number`, or a star projection `*`. */
        public val TypeProjection: KaPiece<KaTypeProjection> = create()

        /** A comma-separated list of type arguments enclosed in angle brackets, e.g. `<Int, String>`. */
        public val TypeArgumentList: KaPiece<List<KaTypeProjection>> = create()

        /** A class type, e.g. `List<String>?`, comprising its [TypeAnnotations], [ClassTypeName], and [TypeNullability]. */
        public val ClassType: KaPiece<KaClassType> = create()

        /**
         * The name of a class type, e.g. `Foo.Bar` or `Int` for `Foo.Bar<Int>`. How the qualified name is rendered is controlled by
         * [KaRenderingOption.ClassTypeQualification].
         */
        public val ClassTypeName: KaPiece<KaClassType> = create()

        /**
         * A function type rendered with function type syntax, e.g. `(Int) -> String`. A nullable or annotated function type is wrapped in
         * parentheses, e.g. `((Int) -> String)?`.
         *
         * By default, a [reflection function type][org.jetbrains.kotlin.analysis.api.types.KaFunctionType.isReflectType], such as
         * `KFunction1<Int, String>`, is rendered as a [ClassType] as there is no type syntax for reflection types.
         */
        public val FunctionType: KaPiece<KaFunctionType> = create()

        /**
         * A single value parameter of a [FunctionType], e.g. `x: Int` in `(x: Int) -> String`, or just `Int` for a parameter which has no
         * name.
         *
         * A function type parameter is named by a synthetic `@ParameterName` annotation on its type, so a parameter only has a name when
         * that annotation is present.
         */
        public val FunctionTypeParameter: KaPiece<KaFunctionValueParameter> = create()

        /** A reference to a type parameter, e.g. `T`. */
        public val TypeParameterType: KaPiece<KaTypeParameterType> = create()

        /** A captured type produced by capturing a projection during type inference, rendered as `Captured(out Number)`. */
        public val CapturedType: KaPiece<KaCapturedType> = create()

        /** A definitely non-nullable type, e.g. `T & Any`. */
        public val DefinitelyNotNullType: KaPiece<KaDefinitelyNotNullType> = create()

        /**
         * A flexible type spanning a lower and an upper bound, such as a platform type. With [KaRenderingOption.FlexibleTypeShrinking]
         * (the default), it is rendered compactly when possible, e.g. `String!`; otherwise as the `(lower..upper)` range of its bounds.
         */
        public val FlexibleType: KaPiece<KaFlexibleType> = create()

        /** An intersection of several types, e.g. `A & B`. */
        public val IntersectionType: KaPiece<KaIntersectionType> = create()

        /** The Kotlin/JS `dynamic` type. */
        public val DynamicType: KaPiece<KaDynamicType> = create()

        /**
         * A type that could not be resolved. An unresolved class type is rendered in its written form, e.g. `C<String>`; other error
         * types are rendered as an error marker.
         */
        public val ErrorType: KaPiece<KaErrorType> = create()

        /**
         * Any [org.jetbrains.kotlin.analysis.api.symbols.KaSymbol]. Dispatches to a more specific symbol piece depending
         * on the symbol kind: [Declaration] for declarations, [Package] for packages, and [File] for files.
         */
        public val Symbol: KaPiece<KaSymbol> = create()

        /** The name of a declaration, as rendered in the declaration itself, e.g. `foo` in `fun foo()`. */
        public val SymbolName: KaPiece<KaNamedSymbol> = create()

        /**
         * All modifiers of a declaration in the canonical Kotlin order, each followed by a space, e.g. `private abstract `.
         * The modifier list can be transformed with [KaRenderingOption.Modifiers], and single modifiers filtered out with
         * [KaRenderingOption.AllowedKeywords].
         */
        public val SymbolModifiers: KaPiece<KaDeclarationSymbol> = create()

        /**
         * Any [org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol]. Dispatches to the piece of its kind: [Callable],
         * [Classifier], [Script], [DestructuringDeclaration], or [ClassInitializer].
         */
        public val Declaration: KaPiece<KaDeclarationSymbol> = create()

        /**
         * A comma-separated list of the declaration's type parameters enclosed in angle brackets, e.g. `<T : Number>`. Nothing is
         * rendered when the declaration has no type parameters.
         */
        public val TypeParameterList: KaPiece<KaDeclarationSymbol> = create()

        /**
         * A single type parameter, including its `reified` modifier, variance, and a single upper bound, e.g. `in T : Number`. Multiple
         * upper bounds are rendered in a [WhereClause] instead.
         */
        public val TypeParameter: KaPiece<KaTypeParameterSymbol> = create()

        /**
         * The `where` clause listing the bounds of type parameters that have more than one upper bound, including the leading space,
         * e.g. ` where T : A, T : B`. Nothing is rendered when no type parameter has multiple bounds.
         */
        public val WhereClause: KaPiece<KaDeclarationSymbol> = create()

        /**
         * Any [org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol]. Dispatches to [Function] for function-like declarations
         * and to [Variable] for variable-like declarations.
         */
        public val Callable: KaPiece<KaCallableSymbol> = create()

        /**
         * The `context(...)` clause of a callable, followed by a line break or a space (see
         * [KaRenderingOption.ContextParametersOnNewLine]). Nothing is rendered when there are no context parameters.
         */
        public val ContextParameterList: KaPiece<KaCallableSymbol> = create()

        /** A single context parameter. */
        public val ContextParameter: KaPiece<KaContextParameterSymbol> = create()

        /** The annotations of a context parameter. */
        public val ContextParameterAnnotations: KaPiece<KaContextParameterSymbol> = create()

        /** The type of context parameter, including the leading colon, e.g. `: String`. */
        public val ContextParameterType: KaPiece<KaContextParameterSymbol> = create()

        /** The parenthesized value parameter list of a function, e.g. `(a: Int, b: String)`. */
        public val ValueParameterList: KaPiece<KaFunctionSymbol> = create()

        /**
         * Any parameter ([org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol]). Dispatches to the piece of its kind:
         * [ValueParameter], [ContextParameter], or [ReceiverParameter].
         */
        public val Parameter: KaPiece<KaParameterSymbol> = create()

        /**
         * A single value parameter, including its modifiers, name, type, and default value. A parameter which declares a property in the
         * primary constructor also carries the property's modifiers and its `val`/`var` (see
         * [KaRenderingOption.PrimaryConstructorInClassHeader]).
         */
        public val ValueParameter: KaPiece<KaValueParameterSymbol> = create()

        /**
         * The annotations of a value parameter. For a parameter which declares a property in the primary constructor, the annotations of
         * the property and its components are also rendered here, with use-site targets, e.g. `@property:Foo`.
         */
        public val ValueParameterAnnotations: KaPiece<KaValueParameterSymbol> = create()

        /** The type of value parameter, including the leading colon, e.g. `: String`. */
        public val ValueParameterType: KaPiece<KaValueParameterSymbol> = create()

        /**
         * The default value of a value parameter, including the leading `=`, e.g. `= ...`. Nothing is rendered for a parameter without
         * a default value.
         */
        public val ValueParameterDefaultValue: KaPiece<KaValueParameterSymbol> = create()

        /**
         * The default value *expression* of a value parameter, without the leading `=`.
         *
         * The expression is not part of the symbol, so it is rendered as an `...` placeholder.
         */
        public val ValueParameterDefaultValueExpression: KaPiece<KaValueParameterSymbol> = create()

        /**
         * Any function-like declaration ([org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol]). Dispatches to the piece of its
         * kind: [NamedFunction], [Constructor] (both primary and secondary), [PropertyAccessor], [SamConstructor],
         * or [AnonymousFunction].
         */
        public val Function: KaPiece<KaFunctionSymbol> = create()

        /** An anonymous function, e.g. `fun(x: Int): String { ... }`. */
        public val AnonymousFunction: KaPiece<KaAnonymousFunctionSymbol> = create()

        /**
         * A SAM constructor: the synthetic function which creates an instance of a functional interface from a lambda,
         * e.g. `fun Runnable(function: () -> Unit): Runnable`.
         */
        public val SamConstructor: KaPiece<KaSamConstructorSymbol> = create()

        /** The annotations of a function. */
        public val FunctionAnnotations: KaPiece<KaFunctionSymbol> = create()

        /**
         * The receiver of an extension callable, including the trailing dot, e.g. `String.` in `fun String.foo()`.
         * A function type or definitely non-nullable receiver is parenthesized, e.g. `(() -> Unit).`.
         */
        public val ReceiverParameter: KaPiece<KaParameterSymbol> = create()

        /** The return type of function, including the leading colon, e.g. `: String`. The type is always rendered. */
        public val FunctionReturnType: KaPiece<KaFunctionSymbol> = create()

        /**
         * The return type of named function, including the leading colon, e.g. `: String`. Nothing is rendered for an implicit `Unit`
         * return type.
         */
        public val NamedFunctionReturnType: KaPiece<KaNamedFunctionSymbol> = create()

        /**
         * The body of a function, including whatever separates it from the signature, e.g. ` { ... }` or ` = 42`.
         * The body is not part of the symbol, so nothing is rendered by default.
         *
         * The piece covers named functions, anonymous functions, SAM constructors, and property accessors.
         * The body of a constructor is rendered as [ConstructorBody] instead.
         */
        public val FunctionBody: KaPiece<KaFunctionSymbol> = create()

        /** A named function declaration. */
        public val NamedFunction: KaPiece<KaNamedFunctionSymbol> = create()

        /** A constructor declaration. */
        public val Constructor: KaPiece<KaConstructorSymbol> = create()

        /**
         * The body of a constructor, including whatever separates it from the signature, e.g. ` { ... }`. The body is not part of the
         * symbol, so nothing is rendered by default.
         *
         * The piece is not rendered for [PrimaryConstructor]s as they do not have a body.
         */
        public val ConstructorBody: KaPiece<KaConstructorSymbol> = create()

        /**
         * The primary constructor of a class, as rendered in the class header, e.g. `(x: Int)` in `class Foo(x: Int)` (see
         * [KaRenderingOption.PrimaryConstructorInClassHeader]).
         *
         * A primary constructor with annotations or an explicit visibility is rendered with a leading space and the `constructor`
         * keyword, as in `class Foo private constructor(x: Int)`. Without parameters, the parentheses are omitted.
         */
        public val PrimaryConstructor: KaPiece<KaConstructorSymbol> = create()

        /**
         * The accessors of a property which are rendered below it, each on its own indented line: the non-default accessors, and the
         * default accessors carrying annotations.
         */
        public val PropertyAccessors: KaPiece<KaPropertySymbol> = create()

        /** A property getter or setter. */
        public val PropertyAccessor: KaPiece<KaPropertyAccessorSymbol> = create()

        /**
         * Any variable-like declaration ([org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol]). Dispatches to the piece of its
         * kind: [Property], [LocalVariable], [JavaField], [BackingField], [EnumEntry], or [Parameter].
         */
        public val Variable: KaPiece<KaVariableSymbol> = create()

        /** A property declaration (`val` or `var`). */
        public val Property: KaPiece<KaPropertySymbol> = create()

        /**
         * The annotations of a property, including those of its backing field and default setter parameter, which have no rendered
         * declaration of their own and so are rendered here with use-site targets, e.g. `@field:Foo`.
         */
        public val PropertyAnnotations: KaPiece<KaPropertySymbol> = create()

        /** The type of property, including the leading colon, e.g. `: String`. */
        public val PropertyReturnType: KaPiece<KaPropertySymbol> = create()

        /**
         * The initializer of a property, including the leading `=`, e.g. `= 42`.
         *
         * Only the initializer of a `const` property is rendered by default, as it is a compile-time constant which is part of the
         * property's declaration in source code.
         */
        public val PropertyInitializer: KaPiece<KaPropertySymbol> = create()

        /** A local variable declaration. */
        public val LocalVariable: KaPiece<KaLocalVariableSymbol> = create()

        /** A field declared in Java. */
        public val JavaField: KaPiece<KaJavaFieldSymbol> = create()

        /** The declaration of a property's backing field, e.g. `field: MutableList<String>`. */
        public val BackingField: KaPiece<KaBackingFieldSymbol> = create()

        /** A single enum entry, including its anonymous object body when present, e.g. `ENTRY { ... }`. */
        public val EnumEntry: KaPiece<KaEnumEntrySymbol> = create()

        /**
         * Any classifier declaration ([org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol]). Dispatches to the piece of its
         * kind: [Class], [TypeAlias], or [TypeParameter].
         */
        public val Classifier: KaPiece<KaClassifierSymbol> = create()

        /**
         * Any class, object, or interface declaration ([org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol]). Dispatches to
         * [NamedClass] or [AnonymousObject].
         */
        public val Class: KaPiece<KaClassSymbol> = create()

        /**
         * A named class, interface, or object declaration. The primary constructor is rendered in the class header when
         * [KaRenderingOption.PrimaryConstructorInClassHeader] is set (the default).
         */
        public val NamedClass: KaPiece<KaNamedClassSymbol> = create()

        /** An anonymous object, e.g. `object : Runnable { ... }`. */
        public val AnonymousObject: KaPiece<KaAnonymousObjectSymbol> = create()

        /** A type alias declaration. */
        public val TypeAlias: KaPiece<KaTypeAliasSymbol> = create()

        /** The annotations of a class. */
        public val ClassAnnotations: KaPiece<KaNamedClassSymbol> = create()

        /**
         * The supertype list of a class, including the leading colon, e.g. ` : Base(), Runnable`. Nothing is rendered when there are no
         * non-trivial supertypes (`Any` and the implicit `Enum`/`Annotation` supertypes are omitted).
         */
        public val SupertypeList: KaPiece<KaClassSymbol> = create()

        /** A single supertype, including the constructor call parentheses of a class supertype, e.g. `Base()`. */
        public val Supertype: KaPiece<KaType> = create()

        /**
         * The body of a class, containing its member declarations, including the braces and the leading space, e.g. ` { ... }`.
         * Nothing is rendered for an empty body. The rendered members and their layout are controlled by [KaRenderingOption.ClassMembers],
         * [KaRenderingOption.ClassMemberOrdering], and [KaRenderingOption.ExtraLineBetweenMembers].
         */
        public val ClassBody: KaPiece<KaClassSymbol> = create()

        /** A Kotlin script. Nothing is rendered by default. */
        public val Script: KaPiece<KaScriptSymbol> = create()

        /**
         * A destructuring declaration, e.g. `val (a, b)`. The names of the created variables are rendered as [SymbolName]s.
         *
         * The declaration's own `val`/`var` keyword and its initializer expression are not part of the symbol: the keyword is taken
         * from the entries, and nothing is rendered after the entry list.
         */
        public val DestructuringDeclaration: KaPiece<KaDestructuringDeclarationSymbol> = create()

        /** A class initializer block, rendered as `init`. */
        public val ClassInitializer: KaPiece<KaClassInitializerSymbol> = create()

        /**
         * A Kotlin file. As a file declares no header of its own, only the file's annotations are rendered, each with the `file`
         * use-site target, e.g. `@file:JvmName("Foo")`.
         */
        public val File: KaPiece<KaFileSymbol> = create()

        /** A package, e.g. `package kotlin.collections`. */
        public val Package: KaPiece<KaPackageSymbol> = create()

        /**
         * The fully-qualified name of a package, rendered segment by segment, e.g. `org.example`.
         *
         * Each segment is linked to the [KaPackageSymbol] of the package it forms, when [KaRenderingOption.LinkSymbols] is enabled and
         * that package can be resolved.
         *
         * The piece renders the segments alone. A dot which separates the package name from what follows, such as the dot in a fully
         * qualified class name, is rendered by the piece that requested it.
         */
        public val PackageName: KaPiece<FqName> = create()

        /**
         * The qualified name of a class, rendered segment by segment, e.g. `Foo.Bar`. How qualified the name is rendered is controlled
         * by [KaRenderingOption.ClassTypeQualification]. Each segment is linked to the classifier it names, when
         * [KaRenderingOption.LinkSymbols] is enabled and it can be resolved.
         *
         * The piece is used in positions which name a class without type arguments, such as annotations. The name of a class *type*,
         * which includes the type arguments of each rendered qualifier segment, is [ClassTypeName].
         */
        public val ClassName: KaPiece<ClassId> = create()
    }
}
