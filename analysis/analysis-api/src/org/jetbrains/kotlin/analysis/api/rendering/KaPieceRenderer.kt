/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
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
import org.jetbrains.kotlin.analysis.api.types.KaIntersectionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection

/**
 * Renders a single [KaPiece] into the contextual [KaRenderingOutput].
 *
 * Renderers for a given [piece] form a stack within a [KaRenderer]; [KaRendererBuilder.push] adds one on top. A renderer may
 * emit nested pieces via the top-level [render] function and fall back to the renderer beneath it by invoking its `next` callback.
 *
 * @param piece the piece this renderer is responsible for.
 */
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
public interface KaRenderingContext {
    /** Renders [value] as the given [piece], dispatching to the corresponding renderer stack. */
    context(session: KaSession)
    public fun <T> render(value: T, piece: KaPiece<T>)

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
public class KaPiece<T> private constructor() {
    @KaExperimentalApi
    public companion object {
        /** The annotations of an annotated element, including the whitespace which separates them from what follows. */
        public val Annotations: KaPiece<KaAnnotated> = KaPiece()

        /** A single annotation entry, e.g. `@Foo(bar)`. */
        public val Annotation: KaPiece<KaAnnotation> = KaPiece()

        /** The parenthesized argument list of an annotation, e.g. `(message = "Use bar")`. */
        public val AnnotationValues: KaPiece<KaAnnotation> = KaPiece()

        /** A single annotation argument value (a constant, enum entry, class literal, nested annotation, or array). */
        public val AnnotationValue: KaPiece<KaAnnotationValue> = KaPiece()

        /** A constant value, e.g. `42`, `"text"`, or `null`. */
        public val ConstantValue: KaPiece<KaConstantValue> = KaPiece()

        /** Any [KaType]. Dispatches to a more specific type piece depending on the type kind. */
        public val Type: KaPiece<KaType> = KaPiece()

        /** The annotations attached to a [KaType]. */
        public val TypeAnnotations: KaPiece<KaType> = KaPiece()

        /** The nullability marker of a type, e.g. `?` in `String?`. Nothing is rendered for a non-nullable type. */
        public val TypeNullability: KaPiece<KaType> = KaPiece()

        /** A single type projection (an explicit type argument or a star projection). */
        public val TypeProjection: KaPiece<KaTypeProjection> = KaPiece()

        /** A comma-separated list of type arguments enclosed in angle brackets, e.g. `<Int, String>`. */
        public val TypeArgumentList: KaPiece<List<KaTypeProjection>> = KaPiece()

        /** A class type, e.g. `List<String>`. */
        public val ClassType: KaPiece<KaClassType> = KaPiece()

        /** A function type, e.g. `(Int) -> String`. */
        public val FunctionType: KaPiece<KaFunctionType> = KaPiece()

        /** A reference to a type parameter, e.g. `T`. */
        public val TypeParameterType: KaPiece<KaTypeParameterType> = KaPiece()

        /** A captured type produced by capturing a projection during type inference. */
        public val CapturedType: KaPiece<KaCapturedType> = KaPiece()

        /** A definitely non-nullable type, e.g. `T & Any`. */
        public val DefinitelyNotNullType: KaPiece<KaDefinitelyNotNullType> = KaPiece()

        /** A flexible type spanning a lower and an upper bound, such as a platform type. */
        public val FlexibleType: KaPiece<KaFlexibleType> = KaPiece()

        /** An intersection of several types, e.g. `A & B`. */
        public val IntersectionType: KaPiece<KaIntersectionType> = KaPiece()

        /** The Kotlin/JS `dynamic` type. */
        public val DynamicType: KaPiece<KaDynamicType> = KaPiece()

        /** A type that could not be resolved. */
        public val ErrorType: KaPiece<KaErrorType> = KaPiece()

        /** Any [KaSymbol]. Dispatches to a more specific symbol piece depending on the symbol kind. */
        public val Symbol: KaPiece<KaSymbol> = KaPiece()

        /** The name of a declaration, as rendered in the declaration itself, e.g. `foo` in `fun foo()`. */
        public val SymbolName: KaPiece<KaSymbol> = KaPiece()

        /** Common modifiers shared by all declarations (visibility, `expect`/`actual`, `external`). */
        public val SymbolModifiers: KaPiece<KaDeclarationSymbol> = KaPiece()

        /** The visibility modifier of a declaration, e.g. `private`. Nothing is rendered for a public or implicit visibility. */
        public val Visibility: KaPiece<KaDeclarationSymbol> = KaPiece()

        /** The modality modifier of a declaration, e.g. `abstract`. Nothing is rendered for a final, implicit, or redundant modality. */
        public val Modality: KaPiece<KaDeclarationSymbol> = KaPiece()

        /** A comma-separated list of type parameters enclosed in angle brackets, e.g. `<T : Number>`. */
        public val TypeParameterList: KaPiece<List<KaTypeParameterSymbol>> = KaPiece()

        /** The type parameter list of a callable. */
        public val CallableTypeParameterList: KaPiece<KaCallableSymbol> = KaPiece()

        /** The type parameter list of a class or a type alias. */
        public val ClassifierTypeParameterList: KaPiece<KaClassLikeSymbol> = KaPiece()

        /** A single type parameter, including its variance, `reified` modifier, and upper bound. */
        public val TypeParameter: KaPiece<KaTypeParameterSymbol> = KaPiece()

        /** The `where` clause listing the bounds of type parameters that have more than one upper bound. */
        public val WhereClause: KaPiece<List<KaTypeParameterSymbol>> = KaPiece()

        /** The `where` clause of a callable. */
        public val CallableWhereClause: KaPiece<KaCallableSymbol> = KaPiece()

        /** The `where` clause of a class or a type alias. */
        public val ClassifierWhereClause: KaPiece<KaClassLikeSymbol> = KaPiece()

        /** A `context(...)` clause. */
        public val ContextParameterList: KaPiece<List<KaContextParameterSymbol>> = KaPiece()

        /** The `context(...)` clause of a callable. */
        public val CallableContextParameterList: KaPiece<KaCallableSymbol> = KaPiece()

        /** A single context parameter. */
        public val ContextParameter: KaPiece<KaContextParameterSymbol> = KaPiece()

        /** The annotations of a context parameter. */
        public val ContextParameterAnnotations: KaPiece<KaContextParameterSymbol> = KaPiece()

        /** The type of context parameter. */
        public val ContextParameterType: KaPiece<KaContextParameterSymbol> = KaPiece()

        /** The parenthesized value parameter list, e.g. `(a: Int, b: String)`. */
        public val ValueParameterList: KaPiece<List<KaValueParameterSymbol>> = KaPiece()

        /** A single value parameter, including its modifiers, name, type, and default value. */
        public val ValueParameter: KaPiece<KaValueParameterSymbol> = KaPiece()

        /** The annotations of a value parameter. */
        public val ValueParameterAnnotations: KaPiece<KaValueParameterSymbol> = KaPiece()

        /** The type of value parameter. */
        public val ValueParameterType: KaPiece<KaValueParameterSymbol> = KaPiece()

        /** The default value of a value parameter, including the leading `=`, e.g. `= ...`. */
        public val ValueParameterDefaultValue: KaPiece<KaValueParameterSymbol> = KaPiece()

        /** Any [KaFunctionSymbol]; used for anonymous functions and SAM constructors. */
        public val Function: KaPiece<KaFunctionSymbol> = KaPiece()

        /** The modifiers of a function, e.g. `suspend`, `operator`, `inline`. */
        public val FunctionModifiers: KaPiece<KaFunctionSymbol> = KaPiece()

        /** The annotations of a function. */
        public val FunctionAnnotations: KaPiece<KaFunctionSymbol> = KaPiece()

        /** The receiver of an extension callable, including the trailing dot, e.g. `String.` in `fun String.foo()`. */
        public val FunctionReceiver: KaPiece<KaParameterSymbol> = KaPiece()

        /** The value parameter list of a function. */
        public val FunctionValueParameterList: KaPiece<KaFunctionSymbol> = KaPiece()

        /** The return type of a function, including the leading colon, e.g. `: String`. The type is always rendered. */
        public val FunctionReturnType: KaPiece<KaFunctionSymbol> = KaPiece()

        /** The return type of named function, including the leading colon, e.g. `: String`. */
        public val NamedFunctionReturnType: KaPiece<KaNamedFunctionSymbol> = KaPiece()

        /** A named function declaration. */
        public val NamedFunction: KaPiece<KaNamedFunctionSymbol> = KaPiece()

        /** A constructor declaration. */
        public val Constructor: KaPiece<KaConstructorSymbol> = KaPiece()

        /** The primary constructor of a class, as rendered in the class header, e.g. `(x: Int)` in `class Foo(x: Int)`. */
        public val PrimaryConstructor: KaPiece<KaConstructorSymbol> = KaPiece()

        /** The non-default accessors of a property, each on its own indented line below the property. */
        public val PropertyAccessors: KaPiece<KaPropertySymbol> = KaPiece()

        /** A property getter or setter. */
        public val PropertyAccessor: KaPiece<KaPropertyAccessorSymbol> = KaPiece()

        /** A property declaration (`val` or `var`). */
        public val Property: KaPiece<KaPropertySymbol> = KaPiece()

        /** The modifiers of a property, e.g. `const`, `lateinit`, `override`. */
        public val PropertyModifiers: KaPiece<KaPropertySymbol> = KaPiece()

        /** The annotations of a property. */
        public val PropertyAnnotations: KaPiece<KaPropertySymbol> = KaPiece()

        /** The type of property. */
        public val PropertyReturnType: KaPiece<KaPropertySymbol> = KaPiece()

        /** A local variable declaration. */
        public val LocalVariable: KaPiece<KaLocalVariableSymbol> = KaPiece()

        /** A field declared in Java. */
        public val JavaField: KaPiece<KaJavaFieldSymbol> = KaPiece()

        /** The backing field of a property. */
        public val BackingField: KaPiece<KaBackingFieldSymbol> = KaPiece()

        /** A single enum entry. */
        public val EnumEntry: KaPiece<KaEnumEntrySymbol> = KaPiece()

        /** A named class, interface, or object declaration. */
        public val NamedClass: KaPiece<KaNamedClassSymbol> = KaPiece()

        /** An anonymous object, e.g. `object : Runnable { ... }`. */
        public val AnonymousObject: KaPiece<KaAnonymousObjectSymbol> = KaPiece()

        /** A type alias declaration. */
        public val TypeAlias: KaPiece<KaTypeAliasSymbol> = KaPiece()

        /** The modifiers of a class, e.g. `data`, `sealed`, `enum`, `inner`. */
        public val ClassModifiers: KaPiece<KaNamedClassSymbol> = KaPiece()

        /** The annotations of a class. */
        public val ClassAnnotations: KaPiece<KaNamedClassSymbol> = KaPiece()

        /** The supertype list of a class, e.g. `: Base, Runnable`. */
        public val SupertypeList: KaPiece<KaClassSymbol> = KaPiece()

        /** A single supertype, including the constructor call parentheses of a class supertype, e.g. `Base()`. */
        public val Supertype: KaPiece<KaType> = KaPiece()

        /** The body of a class, containing its member declarations. */
        public val ClassBody: KaPiece<KaClassSymbol> = KaPiece()

        /** A package, e.g. `package kotlin.collections`. */
        public val Package: KaPiece<KaPackageSymbol> = KaPiece()
    }
}
