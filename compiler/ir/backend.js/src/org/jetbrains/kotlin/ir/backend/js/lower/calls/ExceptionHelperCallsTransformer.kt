/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ExceptionHelperCallsTransformer(private val context: JsIrBackendContext) : CallsTransformer {

    private fun referenceFunction(fqn: FqName) = context.getFunctions(fqn).singleOrNull()?.let {
        context.symbolTable.referenceSimpleFunction(it)
    }

    private val helperMapping = mapOf(
        context.irBuiltIns.throwNpeSymbol to referenceFunction(kotlinPackageFqn.child(Name.identifier("THROW_NPE"))),
        context.irBuiltIns.throwCceSymbol to referenceFunction(kotlinPackageFqn.child(Name.identifier("THROW_CCE"))),
        context.irBuiltIns.throwIseSymbol to referenceFunction(kotlinPackageFqn.child(Name.identifier("THROW_ISE"))),
        context.irBuiltIns.noWhenBranchMatchedExceptionSymbol to referenceFunction(kotlinPackageFqn.child(Name.identifier("noWhenBranchMatchedException")))
    )


    override fun transformCall(call: IrCall) = helperMapping[call.symbol]?.let { irCall(call, it) } ?: call
}
