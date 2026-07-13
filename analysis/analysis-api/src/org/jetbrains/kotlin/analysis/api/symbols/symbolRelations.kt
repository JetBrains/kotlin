/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaAnalysisScopeProvider
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * The [KaSymbol] which contains this symbol, or `null` if there is no containing declaration:
 *
 *  - For top-level declarations, a [KaFileSymbol], or a [KaScriptSymbol] if the file is a script file.
 *  - For [KaScriptSymbol]s, a [KaFileSymbol].
 *  - For class members, the containing class symbol.
 *  - For local declarations, the symbol of the containing declaration.
 */
context(session: KaSession)
public val KaSymbol.containingSymbol: KaSymbol?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.containingSymbol(this)
    }

/**
 * The [KaDeclarationSymbol] which contains this symbol, or `null` if there is no containing declaration:
 *
 *  - For top-level declarations, a containing [KaScriptSymbol], or `null` for non-script declarations.
 *  - For class members, the containing class symbol.
 *  - For local declarations, the symbol of the containing declaration.
 */
context(session: KaSession)
public val KaSymbol.containingDeclaration: KaDeclarationSymbol?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.containingDeclaration(this)
    }

/**
 * The [KaFileSymbol] which contains this symbol, or `null` if this symbol is already a [KaFileSymbol], since it has no containing file.
 * Also `null` for Java and library declarations.
 */
context(session: KaSession)
public val KaSymbol.containingFile: KaFileSymbol?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.containingFile(this)
    }

/**
 * The associated [KaSamConstructorSymbol] if this [KaClassLikeSymbol] is a
 * [functional interface type (SAM)](https://kotlinlang.org/docs/fun-interfaces.html).
 *
 * #### Example
 *
 * ```kotlin
 * fun interface MyPredicate {
 *     fun test(value: Int): Boolean
 * }
 *
 * val p = MyPredicate { it > 0 }  // MyPredicate is a SAM constructor call
 * ```
 *
 * For `MyPredicate`, [samConstructor] is the symbol for the synthetic SAM constructor
 * that enables the `MyPredicate { ... }` lambda syntax.
 */
context(session: KaSession)
public val KaClassLikeSymbol.samConstructor: KaSamConstructorSymbol?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.samConstructor(this)
    }

/**
 * The single abstract function of a [functional interface](https://kotlinlang.org/docs/fun-interfaces.html), or `null` if this class
 * is not a functional interface.
 *
 * A functional interface has exactly one abstract function. In Kotlin, it must be declared with the `fun` modifier.
 * The function may be inherited from a parent interface.
 *
 * #### Example
 *
 * ```kotlin
 * fun interface MyPredicate {
 *     fun test(value: Int): Boolean
 * }
 * ```
 *
 * For `MyPredicate`, [functionalInterfaceFunction] is the symbol for the `test` function.
 *
 * @see KaNamedClassSymbol.isFun
 * @see samConstructor
 */
@KaExperimentalApi
context(session: KaSession)
public val KaClassLikeSymbol.functionalInterfaceFunction: KaNamedFunctionSymbol?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.functionalInterfaceFunction(this)
    }

/**
 * The [KaClassLikeSymbol] of the corresponding [functional (SAM) interface](https://kotlinlang.org/docs/fun-interfaces.html).
 *
 * #### Example
 *
 * ```kotlin
 * fun interface MyPredicate {
 *     fun test(value: Int): Boolean
 * }
 *
 * val p = MyPredicate { it > 0 }  // MyPredicate is a SAM constructor call
 * ```
 *
 * For the `MyPredicate` SAM constructor symbol, [functionalInterface] is the symbol for the `MyPredicate` interface.
 */
context(session: KaSession)
public val KaSamConstructorSymbol.functionalInterface: KaClassLikeSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.functionalInterface(this)
    }

/**
 * The single abstract function of the [functional interface][functionalInterface] that this SAM constructor creates.
 *
 * #### Example
 *
 * ```kotlin
 * fun interface MyPredicate {
 *     fun test(value: Int): Boolean
 * }
 *
 * val p = MyPredicate { it > 0 }  // MyPredicate is a SAM constructor call
 * ```
 *
 * For the `MyPredicate` SAM constructor symbol, [functionalInterfaceFunction] is the symbol for the `test` function.
 *
 * @see KaClassLikeSymbol.functionalInterfaceFunction
 * @see functionalInterface
 */
@KaExperimentalApi
context(session: KaSession)
public val KaSamConstructorSymbol.functionalInterfaceFunction: KaNamedFunctionSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.functionalInterfaceFunction(this)
    }

/**
 * The original [KaConstructorSymbol] for a [type-aliased constructor][KaSymbolOrigin.TYPEALIASED_CONSTRUCTOR], or `null` otherwise.
 *
 * Currently, this property is marked as experimental because it might be joined with [fakeOverrideOriginal] in the future.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaConstructorSymbol.originalConstructorIfTypeAliased: KaConstructorSymbol?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.originalConstructorIfTypeAliased(this)
    }

/**
 * All explicitly declared (non-fake) callable symbols overridden by this callable symbol.
 *
 * The sequence implicitly unwraps substituted and intersection override symbols
 * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE]
 * and [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]).
 *
 * The sequence doesn't include the original overridden declaration of a delegated symbol (for that, use [fakeOverrideOriginal]).
 *
 * Depending on this callable symbol, the sequence contains:
 *
 * - Regular [KaNamedFunctionSymbol] that is not a Java accessor method of a synthetic Java property: overridden function symbols.
 * - Java [KaNamedFunctionSymbol] that corresponds to the getter or setter of a [KaSyntheticJavaPropertySymbol]: the same property
 *   symbols as the corresponding synthetic property accessor, not Java accessor methods.
 * - [KaPropertySymbol], including [KaSyntheticJavaPropertySymbol]: overridden property symbols.
 * - [KaPropertyGetterSymbol]: overridden properties of the containing property, not getter symbols.
 * - [KaPropertySetterSymbol]: overridden mutable properties whose setters are overridden by this setter.
 * - [KaValueParameterSymbol] with [KaValueParameterSymbol.primaryConstructorProperty]: overridden symbols of that generated
 *   property.
 * - Other callable kinds: an empty sequence.
 *
 * The sequence may include [KaSyntheticJavaPropertySymbol]s in Java/Kotlin hierarchies.
 *
 * #### Example
 *
 * ```kotlin
 * abstract class C {
 *     open fun foo() { ... }
 * }
 *
 * abstract class B : C() {
 *     override fun foo() { ... }
 * }
 *
 * class A : B() {
 *     override fun foo() { ... }
 * }
 * ```
 *
 * For `A.foo`, [allOverriddenSymbols] contains both overridden super-declarations, `B.foo` and `C.foo`.
 *
 * @see directlyOverriddenSymbols
 * @see fakeOverrideOriginal
 */
context(session: KaSession)
public val KaCallableSymbol.allOverriddenSymbols: Sequence<KaCallableSymbol>
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.allOverriddenSymbols(this)
    }

/**
 * Explicitly declared (non-fake) callable symbols that are directly overridden by this callable symbol.
 *
 * The sequence implicitly unwraps substituted and intersection override symbols
 * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE]
 * and [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]).
 *
 * The sequence doesn't include the original overridden declaration of a delegated symbol (for that, use [fakeOverrideOriginal]).
 *
 * Symbol kinds follow the same mapping as [allOverriddenSymbols]. In particular, property accessor symbols and Java accessor methods of
 * synthetic Java properties are represented by property symbols rather than accessor or Java method symbols.
 * Setters include only mutable properties whose setters are directly overridden.
 *
 * #### Example
 *
 * ```kotlin
 * abstract class C {
 *     open fun foo() { ... }
 * }
 *
 * abstract class B : C() {
 *     override fun foo() { ... }
 * }
 *
 * class A : B() {
 *     override fun foo() { ... }
 * }
 * ```
 *
 * For `A.foo`, [directlyOverriddenSymbols] contains only the directly overridden super-declaration, `B.foo`.
 *
 * @see allOverriddenSymbols
 * @see fakeOverrideOriginal
 */
context(session: KaSession)
public val KaCallableSymbol.directlyOverriddenSymbols: Sequence<KaCallableSymbol>
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.directlyOverriddenSymbols(this)
    }

/**
 * Checks if [this] class has [superClass] as its superclass somewhere in the inheritance hierarchy.
 *
 * The class is not considered to be a subclass of itself, so `myClass.isSubClassOf(myClass)` is always `false`.
 */
context(session: KaSession)
public fun KaClassSymbol.isSubClassOf(superClass: KaClassSymbol): Boolean {
    @OptIn(KaImplementationDetail::class)
    return internals.symbolRelationProvider.isSubClassOf(this, superClass)
}

/**
 * Checks if [this] class has [superClass] listed as its direct superclass.
 *
 * The class is not considered to be a direct subclass of itself, so `myClass.isDirectSubClassOf(myClass)` is always `false`.
 */
context(session: KaSession)
public fun KaClassSymbol.isDirectSubClassOf(superClass: KaClassSymbol): Boolean {
    @OptIn(KaImplementationDetail::class)
    return internals.symbolRelationProvider.isDirectSubClassOf(this, superClass)
}

/**
 * All callable symbols overridden by this callable symbol if it is an intersection override, or an empty list otherwise.
 *
 * Symbol kinds follow the same mapping as [allOverriddenSymbols]. In particular, property accessor symbols and Java accessor methods of
 * synthetic Java properties are represented by property symbols rather than accessor or Java method symbols.
 * Setters include only mutable properties whose setters are overridden by the intersection override.
 *
 * #### Example
 *
 * ```kotlin
 * interface Foo<T> {
 *     fun foo(value: T)
 * }
 *
 * interface Bar {
 *     fun foo(value: String)
 * }
 *
 * interface Both : Foo<String>, Bar
 * ```
 *
 * The `Both` interface contains an automatically generated intersection override for `foo()`. For it, [intersectionOverriddenSymbols]
 * is a list of two *unsubstituted* symbols: `Foo.foo(T)` and `Bar.foo(String)`.
 *
 * @see KaSymbolOrigin.INTERSECTION_OVERRIDE
 */
context(session: KaSession)
public val KaCallableSymbol.intersectionOverriddenSymbols: List<KaCallableSymbol>
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.intersectionOverriddenSymbols(this)
    }

/**
 * Returns the [KaCallableImplementationState] of the given [KaCallableSymbol] in the context of [implementerClassSymbol].
 *
 * Returns `null` if:
 * - The symbol is a top-level callable;
 * - The symbol is declared in a class or interface that is not a supertype of [implementerClassSymbol];
 * - If the symbol is non-implementable (for example, it is a [KaConstructorSymbol], or a [KaValueParameterSymbol]).
 *
 * The implementation state describes whether a callable is already implemented, has an inherited
 * implementation, can be overridden, or must be explicitly overridden in the given class.
 *
 * @see KaCallableImplementationState
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaCallableSymbol.implementationState(implementerClassSymbol: KaClassSymbol): KaCallableImplementationState? {
    @OptIn(KaImplementationDetail::class)
    return internals.symbolRelationProvider.implementationState(this, implementerClassSymbol)
}

/**
 * The original declared symbol for this callable symbol, after unwrapping fake override [KaCallableSymbol]s if needed.
 *
 * In a class scope, a symbol may be derived from symbols declared in super classes. For example, consider the following:
 *
 * ```
 * public interface A<T> {
 *   public fun foo(t: T)
 * }
 *
 * public interface B : A<String> {
 * }
 * ```
 *
 * In the class scope of `B`, there is a callable symbol `foo` that takes a `String`. This symbol is derived from the original symbol
 * in `A` that takes the type parameter `T` (fake override). Given such a fake override symbol, [fakeOverrideOriginal] is the original
 * declared symbol.
 *
 * Such a situation can also happen for intersection symbols (in case of multiple supertypes containing symbols with an identical
 * signature after specialization) and delegation.
 *
 * @see KaSymbolOrigin.INTERSECTION_OVERRIDE
 * @see KaSymbolOrigin.SUBSTITUTION_OVERRIDE
 * @see KaSymbolOrigin.DELEGATED
 */
context(session: KaSession)
public val KaCallableSymbol.fakeOverrideOriginal: KaCallableSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.fakeOverrideOriginal(this)
    }

/**
 * Returns an `expect` symbol for the given `actual` symbol, if it is available. The function may return multiple `expect` symbols in
 * case of ambiguity errors.
 **/
@KaExperimentalApi
context(session: KaSession)
public fun KaDeclarationSymbol.getExpectsForActual(): List<KaDeclarationSymbol> {
    @OptIn(KaImplementationDetail::class)
    return internals.symbolRelationProvider.getExpectsForActual(this)
}

/**
 * The inheritors of the given sealed class.
 *
 * The list is limited to class symbols which are [analyzable][KaAnalysisScopeProvider.analysisScope] in the use-site [KaModule].
 * While sealed class inheritors can usually only be defined in the same module, there are more complex [rules](https://kotlinlang.org/docs/sealed-classes.html#inheritance-in-multiplatform-projects)
 * around multiplatform projects. If the use-site module is a common source set and additional sealed inheritors are declared in a
 * platform source set, [sealedClassInheritors] will not include those additional platform sealed inheritors.
 *
 * @throws IllegalArgumentException if the given class is not a sealed class.
 */
context(session: KaSession)
public val KaNamedClassSymbol.sealedClassInheritors: List<KaNamedClassSymbol>
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolRelationProvider.sealedClassInheritors(this)
    }

/**
 * Returns whether [this] declaration has a conflicting signature with [other] based on platform-specific checks from [targetPlatform].
 *
 * Note that it doesn't consider function names or their visibility, only their signatures.
 * In other words, it calculates whether two functions would conflict with each other when named equally and positioned in the same scope.
 *
 * Example for JVM platform:
 * ```kotlin
 * fun foo(vararg x: Int) {}
 *
 * fun bar(x: IntArray) {}
 * ```
 *
 * Two functions `foo` and `bar` have the same JVM signature (as vararg parameter is transformed into a regular `IntArray` parameter).
 * However, these two functions can coexist on non-JVM platforms.
 *
 * ``kotlin
 * fun foo() {}
 *
 * fun main() {
 *     fun bar() {}
 * }
 * ```
 *
 * These two functions `foo` and `bar` have signatures, which are conflicting on every platform.
 */
@KaIdeApi
context(session: KaSession)
public fun KaFunctionSymbol.hasConflictingSignatureWith(other: KaFunctionSymbol, targetPlatform: TargetPlatform): Boolean {
    @OptIn(KaImplementationDetail::class)
    return internals.symbolRelationProvider.hasConflictingSignatureWith(this, other, targetPlatform)
}

/**
 * The declaration's type parameters provided it can have them. Otherwise, an empty list.
 *
 * See [Generics](https://kotlinlang.org/docs/generics.html)
 */
public val KaDeclarationSymbol.typeParameters: List<KaTypeParameterSymbol>
    get() = when (this) {
        is KaClassLikeSymbol -> typeParameters
        is KaCallableSymbol -> typeParameters
        else -> emptyList()
    }
