/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.utils.WASM_JS_VARIANT
import org.jetbrains.kotlin.test.utils.WASM_WASI_VARIANT
import org.jetbrains.kotlin.test.utils.withWasmVariant
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
    override val variantChain: TestVariantChain
        get() = super.variantChain.withWasmVariants(wasmTarget)

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        configureForLLWasmDiagnosticsTest(targetPlatform, wasmTarget)
    }
}

/**
 * The variant chain of an LL Wasm diagnostic test over the test data of the JVM LL test with [this] chain: some test data is shared with
 * the JVM tests, so each variant gets its Wasm counterpart, e.g., `[ll, ll.wasmJs]` for `[ll]`.
 *
 * Unlike [withAdditionalVariant][org.jetbrains.kotlin.analysis.test.data.manager.withAdditionalVariant], it adds no standalone `wasmJs`
 * variant: no test writes it, as the Wasm compiler tests write the base test data.
 */
internal fun TestVariantChain.withWasmVariants(wasmTarget: WasmTarget): TestVariantChain {
    val wasmVariant = when (wasmTarget) {
        WasmTarget.JS -> WASM_JS_VARIANT
        WasmTarget.WASI -> WASM_WASI_VARIANT
    }
    return this + map { it.withWasmVariant(wasmVariant) }
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
