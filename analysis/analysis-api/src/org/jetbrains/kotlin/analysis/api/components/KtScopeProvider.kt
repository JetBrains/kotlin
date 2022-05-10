/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.scopes.*
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
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

    public abstract fun getTypeScope(type: KtType): KtScope?

    public abstract fun getScopeContextForPosition(
        originalFile: KtFile,
        positionInFakeFile: KtElement
    ): KtScopeContext
}

public interface KtScopeProviderMixIn : KtAnalysisSessionMixIn {
    public fun KtSymbolWithMembers.getMemberScope(): KtScope =
        analysisSession.scopeProvider.getMemberScope(this)

    public fun KtSymbolWithMembers.getDeclaredMemberScope(): KtScope =
        analysisSession.scopeProvider.getDeclaredMemberScope(this)

    public fun KtSymbolWithMembers.getDelegatedMemberScope(): KtScope =
        analysisSession.scopeProvider.getDelegatedMemberScope(this)

    public fun KtSymbolWithMembers.getStaticMemberScope(): KtScope =
        analysisSession.scopeProvider.getStaticMemberScope(this)

    public fun KtFileSymbol.getFileScope(): KtScope =
        analysisSession.scopeProvider.getFileScope(this)

    public fun KtPackageSymbol.getPackageScope(): KtScope =
        analysisSession.scopeProvider.getPackageScope(this)

    public fun List<KtScope>.asCompositeScope(): KtScope =
        analysisSession.scopeProvider.getCompositeScope(this)

    public fun KtType.getTypeScope(): KtScope? =
        analysisSession.scopeProvider.getTypeScope(this)

    public fun KtFile.getScopeContextForPosition(positionInFakeFile: KtElement): KtScopeContext =
        analysisSession.scopeProvider.getScopeContextForPosition(this, positionInFakeFile)

    public fun KtFile.getScopeContextForFile(): KtScopeContext =
        analysisSession.scopeProvider.getScopeContextForPosition(this, this)
}

public data class KtScopeContext(val scopes: KtScope, val implicitReceivers: List<KtImplicitReceiver>)

public class KtImplicitReceiver(
    override val token: KtLifetimeToken,
    public val type: KtType,
    public val ownerSymbol: KtSymbol
) : KtLifetimeOwner
