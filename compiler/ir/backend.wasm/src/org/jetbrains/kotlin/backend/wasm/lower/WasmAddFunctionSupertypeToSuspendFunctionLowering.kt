/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.AddFunctionSupertypeToSuspendFunctionLowering
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.getAllSubstitutedSupertypes

internal class WasmAddFunctionSupertypeToSuspendFunctionLowering(override val context: WasmBackendContext) :
    AddFunctionSupertypeToSuspendFunctionLowering(context) {

    override fun getLoweredForSuspendFun(irFunction: IrSimpleFunction) =
        irFunction.factory.stageController.restrictTo(irFunction) {
            super.getLoweredForSuspendFun(irFunction)
        }

    override fun transformReturnType(suspendFunctionReturnType: IrType) =
        if (!context.wasmUseStackSwitching) super.transformReturnType(suspendFunctionReturnType) else suspendFunctionReturnType

    override fun addMissingSupertypes(clazz: IrClass) {
        // In Kotlin/Wasm only the (K)SuspendFunctionN <: (K)FunctionN+1 direction is required.
        addFunctionSupertypesToSuspendFunctions(clazz, getAllSubstitutedSupertypes(clazz))
    }
}
