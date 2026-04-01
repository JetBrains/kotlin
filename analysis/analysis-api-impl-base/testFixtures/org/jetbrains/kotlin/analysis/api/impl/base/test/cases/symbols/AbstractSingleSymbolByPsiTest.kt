/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.test.framework.services.AnalysisApiTestOutputSanitizer
import org.jetbrains.kotlin.analysis.test.framework.services.KmpSymbolTestOutputSanitizer
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.platform.wasm.isWasm
import org.jetbrains.kotlin.platform.wasm.isWasmJs
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.bind

abstract class AbstractSingleSymbolByPsiTest : AbstractSymbolTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            when {
                configurator.defaultTargetPlatform.isWasm() -> configureForWasmSymbolTests(configurator.defaultTargetPlatform)
            }

            useAdditionalService<AnalysisApiTestOutputSanitizer> {
                KmpSymbolTestOutputSanitizer(it)
            }
        }
    }

    override fun KaSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeByDirective(
            ktFile, testServices.moduleStructure.modules.first(),
            defaultType = KtDeclaration::class
        )

        val symbol = when (declaration) {
            is KtDeclaration -> declaration.symbol
            is KtFile -> declaration.symbol
            else -> error("Selected element type should be a declaration or a file")
        }

        return SymbolsData(listOf(symbol))
    }
}

private fun TestConfigurationBuilder.configureForWasmSymbolTests(targetPlatform: TargetPlatform) {
    val wasmTarget = when {
        targetPlatform.isWasmJs() -> WasmTarget.JS
        targetPlatform.isWasmWasi() -> WasmTarget.WASI
        else -> error("Unknown wasm target")
    }

    useConfigurators(
        ::WasmFirstStageEnvironmentConfigurator.bind(wasmTarget),
    )
}