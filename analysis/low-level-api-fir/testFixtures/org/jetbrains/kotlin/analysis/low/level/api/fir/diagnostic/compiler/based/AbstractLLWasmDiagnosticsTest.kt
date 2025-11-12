/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind

/**
 * Checks WASM diagnostics in the test data with regular resolution order.
 *
 * A counterpart for [AbstractLLReversedWasmDiagnosticsTest].
 *
 * @see AbstractLLReversedWasmDiagnosticsTest
 */
abstract class AbstractLLWasmDiagnosticsTest(
    private val targetPlatform: TargetPlatform,
    private val wasmTarget: WasmTarget,
) : AbstractLLDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        configureForLLWasmDiagnosticsTest(targetPlatform, wasmTarget)
    }
}

fun TestConfigurationBuilder.configureForLLWasmDiagnosticsTest(targetPlatform: TargetPlatform, wasmTarget: WasmTarget) {
    globalDefaults {
        this.targetPlatform = targetPlatform
        this.targetBackend = TargetBackend.WASM
    }

    useConfigurators(
        ::WasmFirstStageEnvironmentConfigurator.bind(wasmTarget),
    )

    forTestsMatching("compiler/testData/diagnostics/wasmTests/multiplatform/*") {
        defaultDirectives {
            LanguageSettingsDirectives.LANGUAGE + "+MultiPlatformProjects"
        }
    }
}


abstract class AbstractLLWasmJsDiagnosticsTest : AbstractLLWasmDiagnosticsTest(
    WasmPlatforms.wasmJs,
    WasmTarget.JS,
)

abstract class AbstractLLWasmWasiDiagnosticsTest : AbstractLLWasmDiagnosticsTest(
    WasmPlatforms.wasmWasi,
    WasmTarget.WASI,
)


