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

package org.jetbrains.kotlin.backend.js.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.bridges.generateBridgesForFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

class BridgeLowering : ClassLoweringPass {
    private val nameSuggestion = NameSuggestion()

    override fun lower(irClass: IrClass) {
        if (irClass.descriptor.kind == ClassKind.ANNOTATION_CLASS) return

        val functions = irClass.descriptor.unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.CALLABLES)
                .filterIsInstance<FunctionDescriptor>()
                .filterNot { it.modality == Modality.ABSTRACT || it is ConstructorDescriptor || it.dispatchReceiverParameter == null }
        if (functions.isEmpty()) return

        for (function in functions) {
            val bridgesToGenerate = generateBridgesForFunctionDescriptor(
                    function,
                    { SignatureAndDescriptor(nameSuggestion.suggest(it)!!.names.first(), it) },
                    { false })

            for ((fromDescriptor, toDescriptor) in bridgesToGenerate.asSequence().map { (from, to) -> from.descriptor to to.descriptor }) {
                if (fromDescriptor != toDescriptor) {
                    createBridge(irClass, fromDescriptor.original, toDescriptor.original)
                }
            }
        }
    }

    private fun createBridge(irClass: IrClass, fromDescriptor: FunctionDescriptor, toDescriptor: FunctionDescriptor) {
        val newFunction = IrFunctionImpl(irClass.startOffset, irClass.endOffset, irClass.origin, fromDescriptor)
        val delegateCall = IrCallImpl(irClass.startOffset, irClass.endOffset, IrSimpleFunctionSymbolImpl(toDescriptor))
        for ((i, paramDescriptor) in newFunction.descriptor.valueParameters.withIndex()) {
            val irValueParameter = IrValueParameterImpl(
                    irClass.startOffset, irClass.endOffset, irClass.origin,
                    paramDescriptor.original)
            newFunction.valueParameters += irValueParameter
            var getParamExpr: IrExpression = IrGetValueImpl(
                    irClass.startOffset, irClass.endOffset,
                    IrValueParameterSymbolImpl(paramDescriptor))

            val targetType = toDescriptor.valueParameters[i].original.type
            if (paramDescriptor.type != targetType) {
                getParamExpr = IrTypeOperatorCallImpl(
                        irClass.startOffset, irClass.endOffset,
                        paramDescriptor.original.type, IrTypeOperator.IMPLICIT_CAST, targetType, getParamExpr)
            }
            delegateCall.putValueArgument(i, getParamExpr)
        }

        val dispatchReceiver = irClass.thisReceiver!!
        newFunction.dispatchReceiverParameter = dispatchReceiver
        delegateCall.dispatchReceiver = IrGetValueImpl(irClass.startOffset, irClass.endOffset, dispatchReceiver.symbol)

        newFunction.body = IrExpressionBodyImpl(delegateCall)
        irClass.addMember(newFunction)
    }

    internal class SignatureAndDescriptor(val signature: String, val descriptor: FunctionDescriptor) {
        override fun equals(other: Any?): Boolean =
            this === other || signature == (other as SignatureAndDescriptor).signature

        override fun hashCode(): Int = signature.hashCode()
    }
}