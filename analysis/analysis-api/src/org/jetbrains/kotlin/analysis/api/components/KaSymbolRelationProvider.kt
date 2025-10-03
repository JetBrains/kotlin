/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaK1Unsupported
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.util.ImplementationStatus

@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaSymbolRelationProvider : KaSessionComponent {
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
     * The associated [KaSamConstructorSymbol] if this [KaClassLikeSymbol] is a
     * [functional interface type (SAM)](https://kotlinlang.org/docs/fun-interfaces.html).
     */
    public val KaClassLikeSymbol.samConstructor: KaSamConstructorSymbol?

    /**
     * Returns the [KaClassLikeSymbol] of the corresponding SAM interface.
     */
    public val KaSamConstructorSymbol.constructedClass: KaClassLikeSymbol

    /**
     * Returns the original [KaConstructorSymbol] for a [type-aliased constructor][KaSymbolOrigin.TYPEALIASED_CONSTRUCTOR], or `null`
     * otherwise.
     *
     * Currently, this property is marked as experimental because it might be joined with [fakeOverrideOriginal] in the future.
     */
    @KaExperimentalApi
    public val KaConstructorSymbol.originalConstructorIfTypeAliased: KaConstructorSymbol?

    /**
     * A list of **all** explicitly declared symbols that are overridden by the callable symbol.
     *
     * The function doesn't return fake declarations, as it unwraps substituted overridden symbols implicitly
     * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE]
     * and [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]).
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
     * For `A.foo`, [allOverriddenSymbols] returns both overridden super-declarations, `B.foo` and `C.foo`.
     *
     * @see directlyOverriddenSymbols
     */
    public val KaCallableSymbol.allOverriddenSymbols: Sequence<KaCallableSymbol>

    /**
     * A list of explicitly declared symbols which are **directly** overridden by the callable symbol.
     *
     * The function doesn't return fake declarations, as it unwraps substituted overridden symbols implicitly
     * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE]
     * and [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]).
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
     * For `A.foo`, [directlyOverriddenSymbols] returns only the directly overridden super-declaration, `B.foo`.
     *
     * @see allOverriddenSymbols
     */
    public val KaCallableSymbol.directlyOverriddenSymbols: Sequence<KaCallableSymbol>

    /**
     * Checks if [this] class has [superClass] as its superclass somewhere in the inheritance hierarchy.
     *
     * The class is not considered to be a subclass of itself, so `myClass.isSubClassOf(myClass)` is always `false`.
     */
    public fun KaClassSymbol.isSubClassOf(superClass: KaClassSymbol): Boolean

    /**
     * Checks if [this] class has [superClass] listed as its direct superclass.
     *
     * The class is not considered to be a direct subclass of itself, so `myClass.isDirectSubClassOf(myClass)` is always `false`.
     */
    public fun KaClassSymbol.isDirectSubClassOf(superClass: KaClassSymbol): Boolean

    /**
     * If the given callable is an intersection override, returns the list of all overridden symbols. Otherwise, returns an empty list.
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
     * returns a list of two *unsubstituted* symbols: `Foo.foo(T)` and `Bar.foo(Int)`.
     *
     * @see KaSymbolOrigin.INTERSECTION_OVERRIDE
     */
    @KaK1Unsupported
    public val KaCallableSymbol.intersectionOverriddenSymbols: List<KaCallableSymbol>

    /**
     * Returns the [ImplementationStatus] of the given [KaCallableSymbol] in the given [parentClassSymbol], or `null` if this symbol is not
     * a member.
     */
    @KaExperimentalApi
    @KaK1Unsupported
    public fun KaCallableSymbol.getImplementationStatus(parentClassSymbol: KaClassSymbol): ImplementationStatus?

    /**
     * Unwraps fake override [KaCallableSymbol]s until an original declared symbol is uncovered.
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
     * in `A` that takes the type parameter `T` (fake override). Given such a fake override symbol, [fakeOverrideOriginal] recovers the
     * original declared symbol.
     *
     * Such a situation can also happen for intersection symbols (in case of multiple supertypes containing symbols with an identical
     * signature after specialization) and delegation.
     *
     * @see KaSymbolOrigin.INTERSECTION_OVERRIDE
     * @see KaSymbolOrigin.SUBSTITUTION_OVERRIDE
     * @see KaSymbolOrigin.DELEGATED
     */
    public val KaCallableSymbol.fakeOverrideOriginal: KaCallableSymbol

    /**
     * Returns an `expect` symbol for the given `actual` symbol, if it is available. The function may return multiple `expect` symbols in
     * case of ambiguity errors.
     **/
    @KaExperimentalApi
    public fun KaDeclarationSymbol.getExpectsForActual(): List<KaDeclarationSymbol>

    /**
     * The inheritors of the given sealed class.
     *
     * The result is limited to class symbols which are [analyzable][KaAnalysisScopeProvider.analysisScope] in the use-site [KaModule].
     * While sealed class inheritors can usually only be defined in the same module, there are more complex [rules](https://kotlinlang.org/docs/sealed-classes.html#inheritance-in-multiplatform-projects)
     * around multiplatform projects. If the use-site module is a common source set and additional sealed inheritors are declared in a
     * platform source set, [sealedClassInheritors] will not include those additional platform sealed inheritors.
     *
     * @throws IllegalArgumentException if the given class is not a sealed class.
     */
    public val KaNamedClassSymbol.sealedClassInheritors: List<KaNamedClassSymbol>
}

/**
 * The [KaSymbol] which contains this symbol, or `null` if there is no containing declaration:
 *
 *  - For top-level declarations, a [KaFileSymbol], or a [KaScriptSymbol] if the file is a script file.
 *  - For [KaScriptSymbol]s, a [KaFileSymbol].
 *  - For class members, the containing class symbol.
 *  - For local declarations, the symbol of the containing declaration.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public val KaSymbol.containingSymbol: KaSymbol?
    get() = with(s) { containingSymbol }

/**
 * The [KaDeclarationSymbol] which contains this symbol, or `null` if there is no containing declaration:
 *
 *  - For top-level declarations, a containing [KaScriptSymbol], or `null` for non-script declarations.
 *  - For class members, the containing class symbol.
 *  - For local declarations, the symbol of the containing declaration.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public val KaSymbol.containingDeclaration: KaDeclarationSymbol?
    get() = with(s) { containingDeclaration }

/**
 * The [KaFileSymbol] which contains this symbol, or `null` if this symbol is already a [KaFileSymbol], since it has no containing file.
 * Also `null` for Java and library declarations.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public val KaSymbol.containingFile: KaFileSymbol?
    get() = with(s) { containingFile }

/**
 * The [KaModule] which contains this symbol.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public val KaSymbol.containingModule: KaModule
    get() = with(s) { containingModule }

/**
 * The associated [KaSamConstructorSymbol] if this [KaClassLikeSymbol] is a
 * [functional interface type (SAM)](https://kotlinlang.org/docs/fun-interfaces.html).
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public val KaClassLikeSymbol.samConstructor: KaSamConstructorSymbol?
    get() = with(s) { samConstructor }

/**
 * Returns the [KaClassLikeSymbol] of the corresponding SAM interface.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public val KaSamConstructorSymbol.constructedClass: KaClassLikeSymbol
    get() = with(s) { constructedClass }

/**
 * Returns the original [KaConstructorSymbol] for a [type-aliased constructor][KaSymbolOrigin.TYPEALIASED_CONSTRUCTOR], or `null`
 * otherwise.
 *
 * Currently, this property is marked as experimental because it might be joined with [fakeOverrideOriginal] in the future.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public val KaConstructorSymbol.originalConstructorIfTypeAliased: KaConstructorSymbol?
    get() = with(s) { originalConstructorIfTypeAliased }

/**
 * A list of **all** explicitly declared symbols that are overridden by the callable symbol.
 *
 * The function doesn't return fake declarations, as it unwraps substituted overridden symbols implicitly
 * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE]
 * and [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]).
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
 * For `A.foo`, [allOverriddenSymbols] returns both overridden super-declarations, `B.foo` and `C.foo`.
 *
 * @see directlyOverriddenSymbols
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public val KaCallableSymbol.allOverriddenSymbols: Sequence<KaCallableSymbol>
    get() = with(s) { allOverriddenSymbols }

/**
 * A list of explicitly declared symbols which are **directly** overridden by the callable symbol.
 *
 * The function doesn't return fake declarations, as it unwraps substituted overridden symbols implicitly
 * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE]
 * and [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]).
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
 * For `A.foo`, [directlyOverriddenSymbols] returns only the directly overridden super-declaration, `B.foo`.
 *
 * @see allOverriddenSymbols
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public val KaCallableSymbol.directlyOverriddenSymbols: Sequence<KaCallableSymbol>
    get() = with(s) { directlyOverriddenSymbols }

/**
 * Checks if [this] class has [superClass] as its superclass somewhere in the inheritance hierarchy.
 *
 * The class is not considered to be a subclass of itself, so `myClass.isSubClassOf(myClass)` is always `false`.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public fun KaClassSymbol.isSubClassOf(superClass: KaClassSymbol): Boolean {
    return with(s) {
        isSubClassOf(
            superClass = superClass,
        )
    }
}

/**
 * Checks if [this] class has [superClass] listed as its direct superclass.
 *
 * The class is not considered to be a direct subclass of itself, so `myClass.isDirectSubClassOf(myClass)` is always `false`.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public fun KaClassSymbol.isDirectSubClassOf(superClass: KaClassSymbol): Boolean {
    return with(s) {
        isDirectSubClassOf(
            superClass = superClass,
        )
    }
}

/**
 * If the given callable is an intersection override, returns the list of all overridden symbols. Otherwise, returns an empty list.
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
 * returns a list of two *unsubstituted* symbols: `Foo.foo(T)` and `Bar.foo(Int)`.
 *
 * @see KaSymbolOrigin.INTERSECTION_OVERRIDE
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaK1Unsupported
@KaContextParameterApi
context(s: KaSession)
public val KaCallableSymbol.intersectionOverriddenSymbols: List<KaCallableSymbol>
    get() = with(s) { intersectionOverriddenSymbols }

/**
 * Returns the [ImplementationStatus] of the given [KaCallableSymbol] in the given [parentClassSymbol], or `null` if this symbol is not
 * a member.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaK1Unsupported
@KaContextParameterApi
context(s: KaSession)
public fun KaCallableSymbol.getImplementationStatus(parentClassSymbol: KaClassSymbol): ImplementationStatus? {
    return with(s) {
        getImplementationStatus(
            parentClassSymbol = parentClassSymbol,
        )
    }
}

/**
 * Unwraps fake override [KaCallableSymbol]s until an original declared symbol is uncovered.
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
 * in `A` that takes the type parameter `T` (fake override). Given such a fake override symbol, [fakeOverrideOriginal] recovers the
 * original declared symbol.
 *
 * Such a situation can also happen for intersection symbols (in case of multiple supertypes containing symbols with an identical
 * signature after specialization) and delegation.
 *
 * @see KaSymbolOrigin.INTERSECTION_OVERRIDE
 * @see KaSymbolOrigin.SUBSTITUTION_OVERRIDE
 * @see KaSymbolOrigin.DELEGATED
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public val KaCallableSymbol.fakeOverrideOriginal: KaCallableSymbol
    get() = with(s) { fakeOverrideOriginal }

/**
 * Returns an `expect` symbol for the given `actual` symbol, if it is available. The function may return multiple `expect` symbols in
 * case of ambiguity errors.
 **/
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KaDeclarationSymbol.getExpectsForActual(): List<KaDeclarationSymbol> {
    return with(s) {
        getExpectsForActual()
    }
}

/**
 * The inheritors of the given sealed class.
 *
 * The result is limited to class symbols which are [analyzable][KaAnalysisScopeProvider.analysisScope] in the use-site [KaModule].
 * While sealed class inheritors can usually only be defined in the same module, there are more complex [rules](https://kotlinlang.org/docs/sealed-classes.html#inheritance-in-multiplatform-projects)
 * around multiplatform projects. If the use-site module is a common source set and additional sealed inheritors are declared in a
 * platform source set, [sealedClassInheritors] will not include those additional platform sealed inheritors.
 *
 * @throws IllegalArgumentException if the given class is not a sealed class.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public val KaNamedClassSymbol.sealedClassInheritors: List<KaNamedClassSymbol>
    get() = with(s) { sealedClassInheritors }
