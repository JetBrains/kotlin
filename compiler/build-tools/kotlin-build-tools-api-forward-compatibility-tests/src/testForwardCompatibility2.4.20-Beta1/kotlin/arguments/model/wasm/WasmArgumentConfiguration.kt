/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class, ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.wasm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain.Companion.wasm
import org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.ArgumentConfiguration
import org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.wasm.WasmArgumentOperationKind.KLIB
import org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.wasm.WasmArgumentOperationKind.LINKING
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.supportsWasm
import java.nio.file.Paths

internal class WasmArgumentConfiguration<T>(
    kotlinToolchain: KotlinToolchains,
    private val descriptor: WasmArgumentTestDescriptor<T>,
) : ArgumentConfiguration<T>(kotlinToolchain, descriptor) {
    val operationKind: WasmArgumentOperationKind = descriptor.operationKind
    val availableSinceVersion: KotlinReleaseVersion = descriptor.availableSinceVersion
    val argumentValues: List<T> = descriptor.argumentValues
    val argumentRawValues: List<String> = descriptor.argumentRawValues
    val invalidRawValues: List<String> = descriptor.invalidRawValues

    fun isPlatformSupported(): Boolean = kotlinToolchain.supportsWasm()

    fun buildArguments(configure: CommonToolArguments.Builder.() -> Unit = {}): CommonToolArguments {
        return when (descriptor.operationKind) {
            KLIB -> kotlinToolchain.wasm.wasmKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments.configure()
            }.build().compilerArguments
            LINKING -> kotlinToolchain.wasm.wasmLinkingOperationBuilder(Paths.get("input.klib"), Paths.get(".")).apply {
                compilerArguments.configure()
            }.build().compilerArguments
        }
    }

    fun setArgument(arguments: CommonToolArguments.Builder, value: T) {
        descriptor.setArgument(arguments, value)
    }

    fun getArgument(arguments: CommonToolArguments): T = descriptor.getArgument(arguments)
}
