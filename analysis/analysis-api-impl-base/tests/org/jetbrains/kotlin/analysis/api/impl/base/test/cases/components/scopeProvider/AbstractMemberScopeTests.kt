/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithMembers
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractMemberScopeTestBase : AbstractScopeTestBase() {
    abstract fun KaSession.getScope(symbol: KaSymbolWithMembers): KaScope

    final override fun KaSession.getScope(mainFile: KtFile, testServices: TestServices): KaScope =
        getScope(getSingleTestTargetSymbolOfType<KaSymbolWithMembers>(mainFile, testDataPath))
}

abstract class AbstractMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaSymbolWithMembers): KaScope = symbol.getMemberScope()
}

abstract class AbstractStaticMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaSymbolWithMembers): KaScope = symbol.getStaticMemberScope()
}

abstract class AbstractDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaSymbolWithMembers): KaScope = symbol.getDeclaredMemberScope()
}

abstract class AbstractStaticDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaSymbolWithMembers): KaScope = symbol.getStaticDeclaredMemberScope()
}

abstract class AbstractCombinedDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaSymbolWithMembers): KaScope = symbol.getCombinedDeclaredMemberScope()
}

abstract class AbstractDelegateMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaSymbolWithMembers): KaScope = symbol.getDelegatedMemberScope()

    override fun KaSession.getSymbolsFromScope(scope: KaScope): Sequence<KaDeclarationSymbol> = scope.getCallableSymbols()
}
