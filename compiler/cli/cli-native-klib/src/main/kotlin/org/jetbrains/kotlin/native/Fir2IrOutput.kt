/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary

data class Fir2IrOutput(
    val frontendOutput: AllModulesFrontendOutput,
    val symbols: KonanSymbols,
    val fir2irActualizedResult: Fir2IrActualizedResult,
    val usedLibraries: Set<KotlinResolvedLibrary>
)
