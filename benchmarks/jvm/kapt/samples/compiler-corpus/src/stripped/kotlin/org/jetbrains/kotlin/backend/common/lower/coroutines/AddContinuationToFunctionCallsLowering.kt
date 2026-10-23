/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageCase.SuspendableFunctionCallWithoutCoroutineContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSources.File as PLFile

/**
 * Add continuation to suspend function calls.
 *
 * Additionally materialize continuation for `getContinuation` intrinsic calls.
 */
abstract class AbstractAddContinuationToFunctionCallsLowering : BodyLoweringPass {
    protected abstract val context: CommonBackendContext

    protected abstract fun IrSimpleFunction.isContinuationItself(): Boolean

    protected open val IrSimpleFunction.continuationOwner: IrSimpleFunction
        get() = this

    protected open fun suspendFunctionReturnTypeAtCallSite(expression: IrCall, newFun: IrSimpleFunction): IrType = TODO()

    protected open fun loweredSuspendFunctionReturnType(suspendFunReturnType: IrType): IrType = TODO()

    override fun lower(irFile: IrFile) { TODO() }

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }

    // IMPORTANT: May return null only if partial linkage is turned on.
    private fun IrSimpleFunction.getContinuationParameter(): IrValueParameter? { TODO() }

    private fun IrCall.throwLinkageError(file: PLFile): IrCall = TODO()
}
