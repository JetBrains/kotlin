/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractMemberScopeTestBase : AbstractScopeTestBase() {
    abstract fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope

    final override fun KtAnalysisSession.getScope(mainFile: KtFile, testServices: TestServices): KtScope =
        getScope(getSingleTestTargetSymbolOfType<KtSymbolWithMembers>(mainFile, testDataPath))
}

abstract class AbstractMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getMemberScope()
}

abstract class AbstractStaticMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getStaticMemberScope()
}

abstract class AbstractDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getDeclaredMemberScope()
}

abstract class AbstractStaticDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getStaticDeclaredMemberScope()
}

abstract class AbstractCombinedDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getCombinedDeclaredMemberScope()
}

abstract class AbstractDelegateMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getDelegatedMemberScope()

    override fun KtAnalysisSession.getSymbolsFromScope(scope: KtScope): Sequence<KtDeclarationSymbol> = scope.getCallableSymbols()
}
