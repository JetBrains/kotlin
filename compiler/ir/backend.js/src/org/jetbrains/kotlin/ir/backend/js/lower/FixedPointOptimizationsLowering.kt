/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.WhileConditionFoldingLowering
import org.jetbrains.kotlin.ir.backend.js.JsIrOptimizationContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody

/**
 * For lowerings that can be ran in [FixedPointOptimizationsLowering].
 * The lowering should set [hasChanges] if the fixed point loop needs another pass.
 * The flag is reset to false before each [lower] call in the loop.
 */
abstract class ChangeAwareBodyLoweringPass : BodyLoweringPass {
    var hasChanges: Boolean = false
        protected set

    /**
     * Returns whether there should be another pass.
     * In most cases this is the same as returning true if there were any changes to the IR.
     */
    fun changeAwareLower(irBody: IrBody, container: IrDeclaration): Boolean {
        hasChanges = false
        lower(irBody, container)
        return hasChanges
    }
}

class FixedPointOptimizationsLowering(context: JsIrOptimizationContext) : BodyLoweringPass {
    val loop: List<ChangeAwareBodyLoweringPass> = listOf(
    )

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        do {
            var needMore = false
            for (sub in loop) {
                needMore = needMore or sub.changeAwareLower(irBody, container)
            }
        } while (needMore)
    }
}
