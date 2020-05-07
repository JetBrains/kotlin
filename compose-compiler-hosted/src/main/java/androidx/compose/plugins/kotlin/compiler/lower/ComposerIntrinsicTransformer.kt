/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.plugins.kotlin.ComposeFqNames
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComposerIntrinsicTransformer(
    val context: IrPluginContext,
    val functionBodySkipping: Boolean
) :
    IrElementTransformerVoid(),
    FileLoweringPass,
    ModuleLoweringPass {

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol.descriptor.fqNameSafe == ComposeFqNames.CurrentComposerIntrinsic) {
            // since this call was transformed by the ComposerParamTransformer, the first argument
            // to this call is the composer itself. We just replace this expression with the
            // argument expression and we are good.
            if (expression.valueArgumentsCount != 1) {
                print("")
            }
            if (functionBodySkipping) {
                assert(expression.valueArgumentsCount == 2)
            } else {
                assert(expression.valueArgumentsCount == 1)
            }
            val composerExpr = expression.getValueArgument(0)
            if (composerExpr == null) error("Expected non-null composer argument")
            return composerExpr
        }
        return super.visitCall(expression)
    }
}