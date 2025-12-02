/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.test.framework.services.*
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.wasm.*
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.bind

abstract class AbstractSingleSymbolByPsiTest(private val targetPlatform: TargetPlatform) : AbstractSymbolTest() {
    override val suppressPsiBasedFilePointerCheck: Boolean get() = false

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            globalDefaults {
                targetPlatform = this@AbstractSingleSymbolByPsiTest.targetPlatform
            }

            when {
                targetPlatform.isWasm() -> configureForWasmSymbolTests(targetPlatform)
            }

            useAdditionalService<AnalysisApiTestOutputSanitizer> {
                KmpSymbolTestOutputSanitizer(it)
            }
            useAdditionalService<AnalysisApiTestOutputPrefixProvider> {
                KmpSymbolTestOutputPrefixProvider(it)
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

abstract class AbstractSingleSymbolByPsiJVMTest : AbstractSingleSymbolByPsiTest(JvmPlatforms.defaultJvmPlatform)
abstract class AbstractSingleSymbolByPsiJSTest : AbstractSingleSymbolByPsiTest(JsPlatforms.defaultJsPlatform)
abstract class AbstractSingleSymbolByPsiWasmJsTest : AbstractSingleSymbolByPsiTest(WasmPlatforms.wasmJs)
abstract class AbstractSingleSymbolByPsiWasmWasiTest : AbstractSingleSymbolByPsiTest(WasmPlatforms.wasmWasi)
abstract class AbstractSingleSymbolByPsiCommonTest : AbstractSingleSymbolByPsiTest(CommonPlatforms.defaultCommonPlatform)