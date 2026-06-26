/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.checkers

import org.jetbrains.kotlin.backend.common.checkers.CommonKlibDiagnosticContext
import org.jetbrains.kotlin.backend.common.checkers.at
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.getConstArgument
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.getSinglePropertyReference
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName

object NativeVolatileCheck : NativeKlibExpressionsChecker<IrCall> {
    private val intrinsicMap = setOf(
        "COMPARE_AND_SET_FIELD",
        "COMPARE_AND_EXCHANGE_FIELD",
        "GET_AND_SET_FIELD",
        "GET_AND_ADD_FIELD",
        "ATOMIC_GET_FIELD",
        "ATOMIC_SET_FIELD",
    )

    private val typedIntrinsicAnnotation = FqName("kotlin.native.internal.TypedIntrinsic")

    fun IrFunctionAccessExpression.isVolatileIntrinsic(): Boolean {
        if (!symbol.isBound) return false
        val owner = symbol.owner
        val annotation = owner.annotations.findAnnotation(typedIntrinsicAnnotation)
        val value = annotation?.getConstArgument<String>("kind") ?: return false
        return value in intrinsicMap
    }

    override fun check(expression: IrCall, context: CommonKlibDiagnosticContext, reporter: IrDiagnosticReporter) {
        if (!expression.isVolatileIntrinsic()) return

        val extensionReceiverIndex = expression.symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.ExtensionReceiver }
        require(extensionReceiverIndex != -1) { "Extension receiver index not found for call ${expression.render()}" }
        val extensionReceiver = expression.arguments[extensionReceiverIndex]
        val reference = getSinglePropertyReference(extensionReceiver, null) ?: return
        val property = (reference.reflectionTargetSymbol as? IrPropertySymbol)?.owner ?: return
        // If property lies in another module, then it will have `IrExternalPackageFragment` instead of `IrFile`
        val declarationFile = property.getPackageFragment()

        if (declarationFile != context.containingFile) {
            val expressionToReportOn = if (context.inlineBlockStack.isNotEmpty()) context.inlineBlockStack.first() else expression
            reporter
                .at(expressionToReportOn, context)
                .report(NativeKlibErrors.LEAKED_VOLATILE_FIELD, expression)
        }
    }
}
