/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextBase
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType

class KCallableNamePropertyLowering(val context: BackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(KCallableNamePropertyTransformer(this))
    }
}

private class KCallableNamePropertyTransformer(val lower: KCallableNamePropertyLowering) : IrElementTransformerVoid() {

    override fun visitCall(expression: IrCall): IrExpression {
        val callableReference = expression.dispatchReceiver as? IrCallableReference ?: return expression

        //TODO rewrite checking
        val directMember = DescriptorUtils.getDirectMember(expression.descriptor)
        val classDescriptor = directMember.containingDeclaration as? ClassDescriptor ?: return expression
        if (!classDescriptor.defaultType.isKFunctionType) return expression
        if (directMember.name.asString() != "name") return expression

        val receiver = callableReference.dispatchReceiver ?: callableReference.extensionReceiver

        return lower.context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset).run {

            IrCompositeImpl(startOffset, endOffset, context.irBuiltIns.stringType).apply {
                receiver?.let {
                    //put receiver for bound callable reference
                    statements.add(it)
                }

                statements.add(
                    IrConstImpl.string(
                        expression.startOffset,
                        expression.endOffset,
                        context.irBuiltIns.stringType,
                        callableReference.descriptor.name.asString()
                    )
                )
            }

        }

    }


    //TODO move all to common utils
    val KotlinType.isKFunctionType: Boolean
        get() {
            val kind = constructor.declarationDescriptor?.getFunctionalClassKind()
            return kind == FunctionClassDescriptor.Kind.KFunction
        }

    fun BackendContext.createIrBuilder(
        symbol: IrSymbol,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ) =
        DeclarationIrBuilder(this, symbol, startOffset, endOffset)

    class DeclarationIrBuilder(
        backendContext: BackendContext,
        symbol: IrSymbol,
        startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET
    ) : IrBuilderWithScope(
        IrLoweringContext(backendContext),
        Scope(symbol),
        startOffset,
        endOffset
    )

    class IrLoweringContext(backendContext: BackendContext) : IrGeneratorContextBase(backendContext.irBuiltIns)
}
