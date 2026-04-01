/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.AddContinuationToFunctionCallsLowering

@PhasePrerequisites(AddContinuationToNonLocalSuspendFunctionsLowering::class)
class WasmAddContinuationToFunctionCallsLowering(context: WasmBackendContext) : AddContinuationToFunctionCallsLowering(context)