/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.InitializersCleanupLowering
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationPopupLowering
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.PrimaryConstructorLowering

@PhasePrerequisites(
    PrimaryConstructorLowering::class,
    LocalDeclarationPopupLowering::class
)
internal class WasmInitializersLowering(context: WasmBackendContext) : InitializersLowering(context)

@PhasePrerequisites(WasmInitializersLowering::class)
internal class WasmInitializersCleanupLowering(context: CommonBackendContext) : InitializersCleanupLowering(context)