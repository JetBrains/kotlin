/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.konan.ir.annotations.exportedBridgeNonVirtualTargetMethod
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * Rewrites, inside a Swift Export forward bridge marked with `@ExportedBridge(..., nonVirtualTargetMethod = "<target>")`,
 * the single call to `<target>` so it is dispatched "directly" rather than virtually. There are two flavours,
 * both needed by cross-language inheritance:
 *
 * - A call to a regular method is turned into a non-virtual (super-qualified) call, so that Kotlin-side dispatch does not
 *   re-enter a Swift subclass's runtime-patched vtable/itable.
 * - A call to an abstract/sealed class constructor (used by the init bridge to run the abstract constructor on
 *   the Swift subclass's Kotlin backing object via `initInstance`) is turned into an
 *   [org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall]. This is what lets it survive partial
 *   linkage, which rewrites a plain abstract-class [IrConstructorCall] into a linkage error but leaves a
 *   delegating call — mirroring a normal `super(...)` — untouched.
 */
class NativeExportedBridgeCallDispatchLowering(val context: LoweringContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        for (function in irFile.declarations.filterIsInstance<IrSimpleFunction>()) {
            val targetMethod = function.exportedBridgeNonVirtualTargetMethod ?: continue
            function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                    expression.transformChildrenVoid()

                    val callee = expression.symbol.owner
                    if (callee.name.asString() != targetMethod) return expression
                    val constructedClass = callee.parentAsClass
                    if (constructedClass.modality != Modality.ABSTRACT && constructedClass.modality != Modality.SEALED) {
                        return expression
                    }

                    return IrDelegatingConstructorCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        context.irBuiltIns.unitType,
                        expression.symbol,
                        typeArgumentsCount = expression.typeArguments.size,
                    ).apply {
                        copyTypeArgumentsFrom(expression)
                        arguments.assignFrom(expression.arguments)
                    }
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    expression.transformChildrenVoid()

                    val callee = expression.symbol.owner
                    if (callee.name.asString() != targetMethod) return expression
                    if (expression.superQualifierSymbol != null) return expression

                    val superClass = callee.parentClassOrNull ?: return expression
                    if (callee.resolveFakeOverride()?.modality == Modality.ABSTRACT) return expression

                    return IrCallImpl.fromSymbolOwner(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        expression.symbol,
                        origin = expression.origin,
                        superQualifierSymbol = superClass.symbol,
                    ).apply {
                        copyTypeAndValueArgumentsFrom(expression)
                    }
                }
            })
        }
    }
}
