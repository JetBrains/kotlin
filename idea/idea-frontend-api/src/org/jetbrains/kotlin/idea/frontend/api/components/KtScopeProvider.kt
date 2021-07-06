/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.scopes.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithDeclarations
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

public abstract class KtScopeProvider : KtAnalysisSessionComponent() {
    public abstract fun getMemberScope(classSymbol: KtSymbolWithMembers): KtMemberScope
    public abstract fun getStaticMemberScope(symbol: KtSymbolWithMembers): KtScope

    public abstract fun getDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtDeclaredMemberScope
    public abstract fun getFileScope(fileSymbol: KtFileSymbol): KtDeclarationScope<KtSymbolWithDeclarations>
    public abstract fun getPackageScope(packageSymbol: KtPackageSymbol): KtPackageScope
    public abstract fun getCompositeScope(subScopes: List<KtScope>): KtCompositeScope

    public abstract fun getTypeScope(type: KtType): KtScope?

    public abstract fun getScopeContextForPosition(
        originalFile: KtFile,
        positionInFakeFile: KtElement
    ): KtScopeContext
}

public interface KtScopeProviderMixIn : KtAnalysisSessionMixIn {
    public fun KtSymbolWithMembers.getMemberScope(): KtMemberScope =
        analysisSession.scopeProvider.getMemberScope(this)

    public fun KtSymbolWithMembers.getDeclaredMemberScope(): KtDeclaredMemberScope =
        analysisSession.scopeProvider.getDeclaredMemberScope(this)

    public fun KtSymbolWithMembers.getStaticMemberScope(): KtScope =
        analysisSession.scopeProvider.getStaticMemberScope(this)

    public fun KtFileSymbol.getFileScope(): KtDeclarationScope<KtSymbolWithDeclarations> =
        analysisSession.scopeProvider.getFileScope(this)

    public fun KtPackageSymbol.getPackageScope(): KtPackageScope =
        analysisSession.scopeProvider.getPackageScope(this)

    public fun List<KtScope>.asCompositeScope(): KtCompositeScope =
        analysisSession.scopeProvider.getCompositeScope(this)

    public fun KtType.getTypeScope(): KtScope? =
        analysisSession.scopeProvider.getTypeScope(this)

    public fun KtFile.getScopeContextForPosition(positionInFakeFile: KtElement): KtScopeContext =
        analysisSession.scopeProvider.getScopeContextForPosition(this, positionInFakeFile)

    public fun KtFile.getScopeContextForFile(): KtScopeContext =
        analysisSession.scopeProvider.getScopeContextForPosition(this, this)
}

public data class KtScopeContext(val scopes: KtCompositeScope, val implicitReceivers: List<KtImplicitReceiver>)

public class KtImplicitReceiver(
    override val token: ValidityToken,
    public val type: KtType,
    public val ownerSymbol: KtSymbol
) : ValidityTokenOwner
