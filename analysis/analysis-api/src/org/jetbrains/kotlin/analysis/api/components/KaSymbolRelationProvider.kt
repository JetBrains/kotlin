/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.util.ImplementationStatus

public interface KaSymbolRelationProvider {
    /**
     * The [KaSymbol] which contains this symbol, or `null` if there is no containing declaration:
     *
     *  - For top-level declarations, a [KaFileSymbol], or a [KaScriptSymbol] if the file is a script file.
     *  - For [KaScriptSymbol]s, a [KaFileSymbol].
     *  - For class members, the containing class symbol.
     *  - For local declarations, the symbol of the containing declaration.
     */
    public val KaSymbol.containingSymbol: KaSymbol?

    /**
     * The [KaDeclarationSymbol] which contains this symbol, or `null` if there is no containing declaration:
     *
     *  - For top-level declarations, a containing [KaScriptSymbol], or `null` for non-script declarations.
     *  - For class members, the containing class symbol.
     *  - For local declarations, the symbol of the containing declaration.
     */
    public val KaSymbol.containingDeclaration: KaDeclarationSymbol?

    /**
     * The [KaFileSymbol] which contains this symbol, or `null` if this symbol is already a [KaFileSymbol], since it has no containing file.
     * Also `null` for Java and library declarations.
     */
    public val KaSymbol.containingFile: KaFileSymbol?

    /**
     * The [KaModule] which contains this symbol.
     */
    public val KaSymbol.containingModule: KaModule

    /**
     * Returns [KaSamConstructorSymbol] if the given [KaClassLikeSymbol] is a functional interface type, a.k.a. SAM.
     */
    public val KaClassLikeSymbol.samConstructor: KaSamConstructorSymbol?

    /**
     * Returns [KaClassLikeSymbol] of the corresponding SAM interface
     */
    public val KaSamConstructorSymbol.constructedClass: KaClassLikeSymbol

    /**
     * Returns the original [KaConstructorSymbol] for type-aliased constructor, or `null` otherwise.
     *
     * Note: currently this property is marked as experimental, because we might join
     * it with [fakeOverrideOriginal] property in the future.
     *
     * @see KaSymbolOrigin.TYPEALIASED_CONSTRUCTOR
     */
    @KaExperimentalApi
    public val KaConstructorSymbol.originalConstructorIfTypeAliased: KaConstructorSymbol?


    /**
     * A list of **all** explicitly declared symbols that are overridden by symbol
     *
     * E.g., if we have `A.foo` overrides `B.foo` overrides `C.foo`, all two super declarations `B.foo`, `C.foo` will be returned
     *
     * Unwraps substituted overridden symbols
     * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE] and
     * [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]),
     * so such fake declaration won't be returned.
     *
     * @see directlyOverriddenSymbols
     */
    public val KaCallableSymbol.allOverriddenSymbols: Sequence<KaCallableSymbol>

    /**
     * A list of explicitly declared symbols which are **directly** overridden by symbol
     **
     * E.g., if we have `A.foo` overrides `B.foo` overrides `C.foo`, only declarations directly overridden `B.foo` will be returned
     *
     * Unwraps substituted overridden symbols
     * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE] and
     * [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]),
     * so such fake declaration won't be returned.
     *
     *  @see allOverriddenSymbols
     */
    public val KaCallableSymbol.directlyOverriddenSymbols: Sequence<KaCallableSymbol>

    /**
     * Checks if [this] class has [superClass] as its superclass somewhere in the inheritance hierarchy.
     *
     * N.B. The class is not considered to be a subclass of itself, so `myClass.isSubClassOf(myClass)` is always `false`.
     */
    public fun KaClassSymbol.isSubClassOf(superClass: KaClassSymbol): Boolean

    /**
     * Checks if [this] class has [superClass] listed as its direct superclass.
     *
     * N.B. The class is not considered to be a direct subclass of itself, so `myClass.isDirectSubClassOf(myClass)` is always `false`.
     */
    public fun KaClassSymbol.isDirectSubClassOf(superClass: KaClassSymbol): Boolean

    /**
     * The list of all overridden symbols for the given intersection override callable.
     *
     * Example:
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
     * The `Both` interface contains an automatically generated intersection override for `foo()`.
     * For it, [intersectionOverriddenSymbols] will return a list of two *unsubstituted* symbols: `Foo.foo(T)` and `Bar.foo(Int)`.
     */
    public val KaCallableSymbol.intersectionOverriddenSymbols: List<KaCallableSymbol>

    /**
     * Returns the [ImplementationStatus] of the [this] member symbol in the given [parentClassSymbol],
     * or `null` if this symbol is not a member.
     */
    @KaExperimentalApi
    public fun KaCallableSymbol.getImplementationStatus(parentClassSymbol: KaClassSymbol): ImplementationStatus?

    /**
     * Unwraps fake override [KaCallableSymbol]s until an original declared symbol is uncovered.
     *
     * In a class scope, a symbol may be derived from symbols declared in super classes. For example, consider
     *
     * ```
     * public interface  A<T> {
     *   public fun foo(t:T)
     * }
     *
     * public interface  B: A<String> {
     * }
     * ```
     *
     * In the class scope of `B`, there is a callable symbol `foo` that takes a `String`. This symbol is derived from the original symbol
     * in `A` that takes the type parameter `T` (fake override). Given such a fake override symbol, [fakeOverrideOriginal] recovers the
     * original declared symbol.
     *
     * Such situation can also happen for intersection symbols (in case of multiple super types containing symbols with identical signature
     * after specialization) and delegation.
     */
    public val KaCallableSymbol.fakeOverrideOriginal: KaCallableSymbol

    /**
     * Returns `expect` symbols for the given `actual` one if it is available.
     *
     * @return a single expect declaration that corresponds to the [KaDeclarationSymbol] on valid code,
     * or multiple `expect`s in a case of erroneous code with multiple `expect`s.
     **/
    @KaExperimentalApi
    public fun KaDeclarationSymbol.getExpectsForActual(): List<KaDeclarationSymbol>

    /**
     * Inheritors of the given sealed class.
     *
     * @throws IllegalArgumentException if the given class is not a sealed class.
     */
    public val KaNamedClassSymbol.sealedClassInheritors: List<KaNamedClassSymbol>
}