/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.components.declaredMemberScope
import org.jetbrains.kotlin.analysis.api.components.delegatedMemberScope
import org.jetbrains.kotlin.analysis.api.components.memberScope
import org.jetbrains.kotlin.analysis.api.components.staticDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.components.staticMemberScope
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.test.framework.targets.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractMemberScopeTestBase : AbstractScopeTestBase() {
    context(_: KaSession)
    abstract fun getScope(symbol: KaDeclarationContainerSymbol): KaScope

    context(_: KaSession)
    final override fun getScope(mainFile: KtFile, testServices: TestServices): KaScope =
        getScope(getSingleTestTargetSymbolOfType<KaDeclarationContainerSymbol>(testDataPath, mainFile))
}

abstract class AbstractMemberScopeTest : AbstractMemberScopeTestBase() {
    context(_: KaSession)
    override fun getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.memberScope
}

abstract class AbstractStaticMemberScopeTest : AbstractMemberScopeTestBase() {
    context(_: KaSession)
    override fun getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.staticMemberScope
}

abstract class AbstractDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    context(_: KaSession)
    override fun getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.declaredMemberScope
}

abstract class AbstractStaticDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    context(_: KaSession)
    override fun getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.staticDeclaredMemberScope
}

abstract class AbstractCombinedDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    context(_: KaSession)
    override fun getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.combinedDeclaredMemberScope
}

abstract class AbstractDelegateMemberScopeTest : AbstractMemberScopeTestBase() {
    context(_: KaSession)
    override fun getScope(symbol: KaDeclarationContainerSymbol): KaScope = symbol.delegatedMemberScope

    context(_: KaSession)
    override fun getSymbolsFromScope(scope: KaScope): Sequence<KaDeclarationSymbol> = scope.callables
}
