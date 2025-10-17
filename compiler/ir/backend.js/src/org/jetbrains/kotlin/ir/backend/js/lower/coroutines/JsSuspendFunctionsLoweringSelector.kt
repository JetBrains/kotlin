/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class JsSuspendFunctionsLoweringSelector(private val context: JsIrBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        if (context.compileSuspendAsJsGenerator) {
            JsSuspendFunctionWithGeneratorsLowering(context).lower(irModule)
        } else {
            JsSuspendFunctionsLowering(context).lower(irModule)
        }
    }
}