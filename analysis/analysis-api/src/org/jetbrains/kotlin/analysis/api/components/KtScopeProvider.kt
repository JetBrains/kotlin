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
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

public abstract class KtScopeProvider : KtAnalysisSessionComponent() {
    public abstract fun getMemberScope(classSymbol: KtSymbolWithMembers): KtScope

    public abstract fun getDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtScope

    public abstract fun getDelegatedMemberScope(classSymbol: KtSymbolWithMembers): KtScope

    public abstract fun getStaticMemberScope(symbol: KtSymbolWithMembers): KtScope

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
     * Creates [KtScope] containing members of [KtDeclaration].
     * Returned [KtScope] doesn't include synthetic Java properties. To get such properties use [getSyntheticJavaPropertiesScope].
     */
    public fun KtSymbolWithMembers.getMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getMemberScope(this) }

    public fun KtSymbolWithMembers.getDeclaredMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getDeclaredMemberScope(this) }

    public fun KtSymbolWithMembers.getDelegatedMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getDelegatedMemberScope(this) }

    public fun KtSymbolWithMembers.getStaticMemberScope(): KtScope =
        withValidityAssertion { analysisSession.scopeProvider.getStaticMemberScope(this) }

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
