/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ExceptionHelperCallsTransformer(private val context: JsIrBackendContext) : CallsTransformer {

    private fun referenceFunction(fqn: FqName) =
        context.getFunctions(fqn).singleOrNull()?.let {
            context.symbolTable.referenceSimpleFunction(it)
        } ?: throw AssertionError("Function not found: $fqn")

    private val helperMapping = mapOf(
        context.irBuiltIns.checkNotNullSymbol to referenceFunction(kotlinPackageFqn.child(Name.identifier("ensureNotNull"))),
        context.irBuiltIns.throwCceSymbol to referenceFunction(kotlinPackageFqn.child(Name.identifier("THROW_CCE"))),
        context.irBuiltIns.throwIseSymbol to referenceFunction(kotlinPackageFqn.child(Name.identifier("THROW_ISE"))),
        context.irBuiltIns.illegalArgumentExceptionSymbol to referenceFunction(kotlinPackageFqn.child(Name.identifier("THROW_IAE"))),
        context.irBuiltIns.noWhenBranchMatchedExceptionSymbol to referenceFunction(kotlinPackageFqn.child(Name.identifier("noWhenBranchMatchedException")))
    )

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean) =
        helperMapping[call.symbol]?.let { irCall(call, it) } ?: call
}
