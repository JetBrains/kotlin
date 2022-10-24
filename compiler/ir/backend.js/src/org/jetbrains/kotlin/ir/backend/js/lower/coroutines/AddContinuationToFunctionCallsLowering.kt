/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.lower.coroutines.AbstractAddContinuationToFunctionCallsLowering
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter

/**
 * Requires [AddContinuationToLocalSuspendFunctionsLowering] and
 * [AddContinuationToNonLocalSuspendFunctionsLowering] to transform function declarations first.
 */
class AddContinuationToFunctionCallsLowering(override val context: JsCommonBackendContext) : AbstractAddContinuationToFunctionCallsLowering() {
    override fun IrSimpleFunction.getContinuationParameter(): IrValueParameter =
        if (overriddenSymbols.any {
                it.owner.name.asString() == "doResume" && it.owner.parent == context.coroutineSymbols.coroutineImpl.owner
            }
        ) {
            dispatchReceiverParameter!!
        } else {
            valueParameters.last()
        }
}