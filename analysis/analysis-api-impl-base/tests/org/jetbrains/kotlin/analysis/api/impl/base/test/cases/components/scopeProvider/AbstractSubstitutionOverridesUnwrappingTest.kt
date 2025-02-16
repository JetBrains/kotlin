/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolsData
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractSubstitutionOverridesUnwrappingTest : AbstractSymbolTest() {

    override fun KaSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        val declarationUnderCaret = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtClassLikeDeclaration>(ktFile)
        val classSymbolUnderCaret = declarationUnderCaret.symbol as KaClassLikeSymbol

        require(classSymbolUnderCaret is KaDeclarationContainerSymbol)

        return SymbolsData(classSymbolUnderCaret.memberScope.declarations.toList())
    }

    override fun KaSession.renderSymbolForComparison(symbol: KaSymbol, directives: RegisteredDirectives): String {
        return DebugSymbolRenderer().renderForSubstitutionOverrideUnwrappingTest(this@renderSymbolForComparison, symbol)
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