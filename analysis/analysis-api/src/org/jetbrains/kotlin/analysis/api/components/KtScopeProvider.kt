/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtTypeScope
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

public abstract class KtScopeProvider : KtAnalysisSessionComponent() {
    public abstract fun getMemberScope(classSymbol: KtSymbolWithMembers): KtScope

    public abstract fun getStaticMemberScope(symbol: KtSymbolWithMembers): KtScope

    public open fun getCombinedMemberScope(symbol: KtSymbolWithMembers): KtScope = getCompositeScope(
        listOf(
            getMemberScope(symbol),
            getStaticMemberScope(symbol),
        )
    )

    public abstract fun getDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtScope

    public abstract fun getStaticDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtScope

    public abstract fun getCombinedDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtScope

    public abstract fun getDelegatedMemberScope(classSymbol: KtSymbolWithMembers): KtScope

    public abstract fun getEmptyScope(): KtScope

    public abstract fun getFileScope(fileSymbol: KtFileSymbol): KtScope

    public abstract fun getPackageScope(packageSymbol: KtPackageSymbol): KtScope

    public abstract fun getCompositeScope(subScopes: List<KtScope>): KtScope

    public abstract fun getTypeScope(type: KtType): KtTypeScope?

    public abstract fun getSyntheticJavaPropertiesScope(type: KtType): KtTypeScope?

    public abstract fun getImportingScopeContext(file: KtFile): KtScopeContext

    public abstract fun getScopeContextForPosition(
        originalFile: KtFile,
        positionInFakeFile: KtElement
    ): KtScopeContext
}

public interface KtScopeProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Returns a [KtScope] containing *non-static* callable members (functions, properties, and constructors) and all classifier members
     * (classes and objects) of the given [KtSymbolWithMembers]. The scope includes members inherited from the symbol's supertypes, in
     * addition to members which are declared explicitly inside the symbol's body.
     *
     * The member scope doesn't include synthetic Java properties. To get such properties, use [getSyntheticJavaPropertiesScope].
     *
     * @see getStaticMemberScope
     */
    public fun KtSymbolWithMembers.getMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getMemberScope(this) }

    /**
     * Returns a [KtScope] containing the *static* members of the given [KtSymbolWithMembers].
     *
     * The behavior of the scope differs based on whether the given [KtSymbolWithMembers] is a Kotlin or Java class:
     *
     * - **Kotlin class:** The scope contains static callables (functions and properties) and classifiers (classes and objects) declared
     *   directly in the [KtSymbolWithMembers]. Hence, the static member scope for Kotlin classes is equivalent to [getDeclaredMemberScope].
     * - **Java class:** The scope contains static callables (functions and properties) declared in the [KtSymbolWithMembers] or any of its
     *   superclasses (excluding static callables from super-interfaces), and classes declared directly in the [KtSymbolWithMembers]. This
     *   follows Kotlin's rules about static inheritance in Java classes, where static callables are propagated from superclasses, but
     *   nested classes are not.
     *
     * #### Kotlin Example
     *
     * ```kotlin
     * abstract class A {
     *     class C1
     *     inner class D1
     *     object O1
     *
     *     // There is no way to declare a static callable in an abstract class, as only enum classes define additional static callables.
     * }
     *
     * class B : A() {
     *     class C2
     *     inner class D2
     *     object O2
     *     companion object {
     *         val baz: String = ""
     *     }
     * }
     * ```
     *
     * The static member scope of `B` contains the following symbols:
     *
     * ```
     * class C2
     * inner class D2
     * object O2
     * companion object
     * ```
     *
     * #### Java Example
     *
     * ```java
     * // SuperInterface.java
     * public interface SuperInterface {
     *     public static void fromSuperInterface() { }
     * }
     *
     * // SuperClass.java
     * public abstract class SuperClass implements SuperInterface {
     *     static class NestedSuperClass { }
     *     class InnerSuperClass { }
     *     public static void fromSuperClass() { }
     * }
     *
     * // FILE: JavaClass.java
     * public class JavaClass extends SuperClass {
     *     static class NestedClass { }
     *     class InnerClass { }
     *     public static void fromJavaClass() { }
     * }
     * ```
     *
     * The static member scope of `JavaClass` contains the following symbols:
     *
     * ```
     * public static void fromSuperClass()
     * public static void fromJavaClass()
     * static class NestedClass
     * class InnerClass
     * ```
     *
     * @see getMemberScope
     */
    public fun KtSymbolWithMembers.getStaticMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getStaticMemberScope(this) }

    /**
     * Returns a [KtScope] containing all members from [getMemberScope] and [getStaticMemberScope].
     */
    public fun KtSymbolWithMembers.getCombinedMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getCombinedMemberScope(this) }

    /**
     * Returns a [KtScope] containing the *non-static* callables (functions, properties, and constructors) and inner classes explicitly
     * declared in the given [KtSymbolWithMembers].
     *
     * The declared member scope does not contain classifiers (including the companion object) except for inner classes. To retrieve the
     * classifiers declared in this [KtSymbolWithMembers], please use the *static* declared member scope provided by
     * [getStaticDeclaredMemberScope].
     *
     * @see getStaticDeclaredMemberScope
     */
    public fun KtSymbolWithMembers.getDeclaredMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getDeclaredMemberScope(this) }

    /**
     * Returns a [KtScope] containing the *static* callables (functions and properties) and all classifiers (classes and objects) explicitly
     * declared in the given [KtSymbolWithMembers].
     *
     * It is worth noting that, while Java classes may contain declarations of static callables freely, in Kotlin only enum classes define
     * static callables. Hence, for non-enum Kotlin classes, it is not expected that the static declared member scope will contain any
     * callables.
     *
     * @see getDeclaredMemberScope
     */
    public fun KtSymbolWithMembers.getStaticDeclaredMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getStaticDeclaredMemberScope(this) }

    /**
     * Returns a [KtScope] containing *all* members explicitly declared in the given [KtSymbolWithMembers].
     *
     * In contrast to [getDeclaredMemberScope] and [getStaticDeclaredMemberScope], this scope contains both static and non-static members.
     */
    public fun KtSymbolWithMembers.getCombinedDeclaredMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getCombinedDeclaredMemberScope(this) }

    public fun KtSymbolWithMembers.getDelegatedMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getDelegatedMemberScope(this) }

    public fun KtFileSymbol.getFileScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getFileScope(this) }

    public fun KtPackageSymbol.getPackageScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getPackageScope(this) }

    public fun List<KtScope>.asCompositeScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getCompositeScope(this) }

    /**
     * Return a [KtTypeScope] for a given [KtType].
     * The type scope will include all members which are declared and callable on a given type.
     *
     * Comparing to the [KtScope], in the [KtTypeScope] all use-site type parameters are substituted.
     *
     * Consider the following code
     * ```
     * fun foo(list: List<String>) {
     *      list // get KtTypeScope for it
     * }
     *```
     *
     * Inside the `LIST_KT_ELEMENT.getKtType().getTypeScope()` would contain the `get(i: Int): String` method with substituted type `T = String`
     *
     * @return type scope for the given type if given `KtType` is not error type, `null` otherwise.
     * Returned [KtTypeScope] includes synthetic Java properties.
     *
     * @see KtTypeScope
     * @see KtTypeProviderMixIn.getKtType
     */
    public fun KtType.getTypeScope(): KtTypeScope? =
        withValidityAssertion { analysisSession.scopeProvider.getTypeScope(this) }

    /**
     * Returns a [KtTypeScope] with synthetic Java properties created for a given [KtType].
     */
    public fun KtType.getSyntheticJavaPropertiesScope(): KtTypeScope? =
        withValidityAssertion { analysisSession.scopeProvider.getSyntheticJavaPropertiesScope(this) }

    /**
     * For each scope in [KtScopeContext] an index is calculated. The indexes are relative to position, and they are only known for
     * scopes obtained with [getScopeContextForPosition].
     *
     * Scopes with [KtScopeKind.TypeScope] include synthetic Java properties.
     */
    public fun KtFile.getScopeContextForPosition(positionInFakeFile: KtElement): KtScopeContext =
        withValidityAssertion { analysisSession.scopeProvider.getScopeContextForPosition(this, positionInFakeFile) }

    /**
     * Returns a [KtScopeContext] formed by all imports in the [KtFile].
     *
     * By default, this will also include default importing scopes, which can be filtered by [KtScopeKind]
     */
    public fun KtFile.getImportingScopeContext(): KtScopeContext =
        withValidityAssertion { analysisSession.scopeProvider.getImportingScopeContext(this) }

    /**
     * Returns single scope, containing declarations from all scopes that satisfy [filter]. The order of declarations corresponds to the
     * order of their containing scopes, which are sorted according to their indexes in scope tower.
     */
    public fun KtScopeContext.getCompositeScope(filter: (KtScopeKind) -> Boolean = { true }): KtScope = withValidityAssertion {
        val subScopes = scopes.filter { filter(it.kind) }.map { it.scope }
        subScopes.asCompositeScope()
    }
}

public class KtScopeContext(
    private val _scopes: List<KtScopeWithKind>,
    private val _implicitReceivers: List<KtImplicitReceiver>,
    override val token: KtLifetimeToken
) : KtLifetimeOwner {
    public val implicitReceivers: List<KtImplicitReceiver> get() = withValidityAssertion { _implicitReceivers }

    /**
     * Scopes for position, sorted according to their indexes in scope tower, i.e. the first scope is the closest one to position.
     */
    public val scopes: List<KtScopeWithKind> get() = withValidityAssertion { _scopes }
}

public class KtImplicitReceiver(
    override val token: KtLifetimeToken,
    private val _type: KtType,
    private val _ownerSymbol: KtSymbol,
    private val _receiverScopeIndexInTower: Int
) : KtLifetimeOwner {
    public val ownerSymbol: KtSymbol get() = withValidityAssertion { _ownerSymbol }
    public val type: KtType get() = withValidityAssertion { _type }
    public val scopeIndexInTower: Int get() = withValidityAssertion { _receiverScopeIndexInTower }
}


public sealed class KtScopeKind {
    /**
     * Index in scope tower. For example:
     * ```
     * fun f(a: A, b: B) {      // local scope:       indexInTower = 2
     *     with(a) {            // type scope for A:  indexInTower = 1
     *         with(b) {        // type scope for B:  indexInTower = 0
     *             <caret>
     *         }
     *     }
     * }
     * ```
     */
    public abstract val indexInTower: Int

    public class LocalScope(override val indexInTower: Int) : KtScopeKind()

    /**
     * Represents [KtScope] for type, which include synthetic Java properties of corresponding type.
     */
    public class TypeScope(override val indexInTower: Int) : KtScopeKind()

    public sealed class NonLocalScope : KtScopeKind()

    /**
     * Represents [KtScope] containing type parameters.
     */
    public class TypeParameterScope(override val indexInTower: Int) : NonLocalScope()

    /**
     * Represents [KtScope] containing declarations from package.
     */
    public class PackageMemberScope(override val indexInTower: Int) : NonLocalScope()

    /**
     * Represents [KtScope] containing declarations from imports.
     */
    public sealed class ImportingScope : NonLocalScope()

    /**
     * Represents [KtScope] containing declarations from explicit non-star imports.
     */
    public class ExplicitSimpleImportingScope(override val indexInTower: Int) : ImportingScope()

    /**
     * Represents [KtScope] containing declarations from explicit star imports.
     */
    public class ExplicitStarImportingScope(override val indexInTower: Int) : ImportingScope()

    /**
     * Represents [KtScope] containing declarations from non-star imports which are not declared explicitly and are added by default.
     */
    public class DefaultSimpleImportingScope(override val indexInTower: Int) : ImportingScope()

    /**
     * Represents [KtScope] containing declarations from star imports which are not declared explicitly and are added by default.
     */
    public class DefaultStarImportingScope(override val indexInTower: Int) : ImportingScope()

    /**
     * Represents [KtScope] containing static members of a classifier.
     */
    public class StaticMemberScope(override val indexInTower: Int) : NonLocalScope()

    /**
     * Represents [KtScope] containing members of a script.
     */
    public class ScriptMemberScope(override val indexInTower: Int) : NonLocalScope()
}

public data class KtScopeWithKind(
    private val _scope: KtScope,
    private val _kind: KtScopeKind,
    override val token: KtLifetimeToken
) : KtLifetimeOwner {
    public val scope: KtScope get() = withValidityAssertion { _scope }
    public val kind: KtScopeKind get() = withValidityAssertion { _kind }
}
