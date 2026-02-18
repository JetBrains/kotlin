/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.DefaultParameterInjector
import org.jetbrains.kotlin.backend.common.lower.InnerClassesLowering
import org.jetbrains.kotlin.backend.common.lower.MaskedDefaultArgumentFunctionFactory
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext

@PhasePrerequisites(InnerClassesLowering::class)
class WasmDefaultParameterInjector(
    context: WasmBackendContext
) : DefaultParameterInjector<WasmBackendContext>(context, MaskedDefaultArgumentFunctionFactory(context), skipExternalMethods = true)