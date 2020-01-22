/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin.compiler.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid


class ComposeSymbolPatcherTransformer(val context: JvmBackendContext) :
    IrElementTransformerVoid(),
    FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        // NOTE(lmr): for some reason, there are calls getting generated with symbols pointing to
        // the wrong IrFunction (the original one, and not the transformed one). This appears to be
        // related to the File-level class transform that kotlin does, turning the file level
        // declarations into a class. It is unclear if there is a better way to fix these issues or
        // not, but this seems to work reasonably well as a temporary fix.
        val functionSymbol = expression.symbol
        val function = functionSymbol.owner
        val functionParent = function.parent
        if (functionParent is IrFile) {
            val fnInFile = functionParent
                .declarations
                .firstOrNull {
                    it is IrFunction && it.descriptor === function.descriptor
                } as? IrFunction

            if (fnInFile != null && fnInFile != function) {
                return with(expression) {
                    IrCallImpl(
                        startOffset,
                        endOffset,
                        type,
                        fnInFile.symbol,
                        descriptor,
                        origin,
                        superQualifierSymbol
                    ).also {
                        it.copyTypeAndValueArgumentsFrom(this)
                        it.dispatchReceiver = dispatchReceiver
                        it.extensionReceiver = extensionReceiver
                        it.copyAttributes(this)
                    }
                }
            }
        }
        return super.visitCall(expression)
    }
}