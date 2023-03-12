/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolsData
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractSubstitutionOverridesUnwrappingTest : AbstractSymbolTest() {

    override fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        val declarationUnderCaret = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtClassLikeDeclaration>(ktFile)
        val classSymbolUnderCaret = declarationUnderCaret.getSymbol() as KtClassLikeSymbol

        require(classSymbolUnderCaret is KtSymbolWithMembers)

        return SymbolsData(classSymbolUnderCaret.getMemberScope().getAllSymbols().toList())
    }

    override fun KtAnalysisSession.renderSymbolForComparison(symbol: KtSymbol, directives: RegisteredDirectives): String {
        return with(DebugSymbolRenderer()) { renderForSubstitutionOverrideUnwrappingTest(symbol) }
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
            }
        }
    }
}