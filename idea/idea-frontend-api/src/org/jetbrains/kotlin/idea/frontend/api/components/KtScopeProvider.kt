/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.scopes.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithDeclarations
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

abstract class KtScopeProvider : KtAnalysisSessionComponent() {
    abstract fun getMemberScope(classSymbol: KtSymbolWithMembers): KtMemberScope
    abstract fun getDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtDeclaredMemberScope
    abstract fun getFileScope(fileSymbol: KtFileSymbol): KtDeclarationScope<KtSymbolWithDeclarations>
    abstract fun getPackageScope(packageSymbol: KtPackageSymbol): KtPackageScope
    abstract fun getCompositeScope(subScopes: List<KtScope>): KtCompositeScope

    abstract fun getTypeScope(type: KtType): KtScope?

    abstract fun getScopeContextForPosition(
        originalFile: KtFile,
        positionInFakeFile: KtElement
    ): KtScopeContext
}

interface KtScopeProviderMixIn : KtAnalysisSessionMixIn {
    fun KtSymbolWithMembers.getMemberScope(): KtMemberScope =
        analysisSession.scopeProvider.getMemberScope(this)

    fun KtSymbolWithMembers.getDeclaredMemberScope(): KtDeclaredMemberScope =
        analysisSession.scopeProvider.getDeclaredMemberScope(this)

    fun KtFileSymbol.getFileScope(): KtDeclarationScope<KtSymbolWithDeclarations> =
        analysisSession.scopeProvider.getFileScope(this)

    fun KtPackageSymbol.getPackageScope(): KtPackageScope =
        analysisSession.scopeProvider.getPackageScope(this)

    fun List<KtScope>.asCompositeScope(): KtCompositeScope =
        analysisSession.scopeProvider.getCompositeScope(this)

    fun KtType.getTypeScope(): KtScope? =
        analysisSession.scopeProvider.getTypeScope(this)

    fun KtFile.getScopeContextForPosition(positionInFakeFile: KtElement): KtScopeContext =
        analysisSession.scopeProvider.getScopeContextForPosition(this, positionInFakeFile)

    fun KtFile.getScopeContextForFile(): KtScopeContext =
        analysisSession.scopeProvider.getScopeContextForPosition(this, this)
}

data class KtScopeContext(val scopes: KtCompositeScope, val implicitReceiversTypes: List<KtType>)
