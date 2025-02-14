/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.lower.KlibAssertionWrapperLowering
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

@PhaseDescription("NativeAssertionWrapperLowering")
class NativeAssertionWrapperLowering(context: LoweringContext) : KlibAssertionWrapperLowering(context) {
    override val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol = (context.symbols as KonanSymbols).isAssertionArgumentEvaluationEnabled
}