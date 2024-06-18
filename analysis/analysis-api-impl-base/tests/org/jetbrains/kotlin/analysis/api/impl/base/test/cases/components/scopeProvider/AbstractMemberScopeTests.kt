/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractMemberScopeTestBase : AbstractScopeTestBase() {
    abstract fun KaSession.getScope(symbol: KaDeclarationContainerSymbol): KaScope

    final override fun KaSession.getScope(mainFile: KtFile, testServices: TestServices): KaScope =
        getScope(getSingleTestTargetSymbolOfType<KaDeclarationContainerSymbol>(mainFile, testDataPath))
}

abstract class AbstractMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.memberScope
}

abstract class AbstractStaticMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.staticMemberScope
}

abstract class AbstractDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.declaredMemberScope
}

abstract class AbstractStaticDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.staticDeclaredMemberScope
}

abstract class AbstractCombinedDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.combinedDeclaredMemberScope
}

abstract class AbstractDelegateMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KaSession.getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.delegatedMemberScope

    override fun KaSession.getSymbolsFromScope(scope: KaScope): Sequence<KaDeclarationSymbol> = scope.callables
}
