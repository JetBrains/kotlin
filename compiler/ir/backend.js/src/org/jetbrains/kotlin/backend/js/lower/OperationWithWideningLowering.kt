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

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType

class OperationWithWideningLowering : BodyLoweringPass, IrElementTransformerVoid() {
    private val relevantMemberNames = setOf("plus", "minus", "times", "div", "rem", "compareTo", "rangeTo")

    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression =
            tryTransform(expression) ?: expression.also { it.transformChildrenVoid(this) }

    private fun tryTransform(expression: IrCall): IrExpression? {
        val function = expression.descriptor
        if (function.valueParameters.size != 1) return null
        if (function.name.isSpecial || function.name.asString() !in relevantMemberNames) return null

        val firstType = function.dispatchReceiverParameter?.type ?: return null
        val secondType = function.valueParameters[0].type

        val firstPrimitive = KotlinBuiltIns.getPrimitiveType(firstType) ?: return null
        val secondPrimitive = KotlinBuiltIns.getPrimitiveType(secondType) ?: return null
        if (firstPrimitive == secondPrimitive) return null

        var firstArg = expression.dispatchReceiver ?: return null
        var secondArg = expression.getValueArgument(0)!!

        firstArg = firstArg.transform(this, null)
        secondArg = secondArg.transform(this, null)

        val commonType = when {
            firstPrimitive.canBeWidenedTo(secondPrimitive) -> {
                firstArg = firstType.widenTo(secondPrimitive, firstArg, expression.origin)
                secondPrimitive
            }
            secondPrimitive.canBeWidenedTo(firstPrimitive) -> {
                secondArg = secondType.widenTo(firstPrimitive, secondArg, expression.origin)
                firstPrimitive
            }
            else -> return null
        }

        val module = function.module
        val wideClass = module.findClassAcrossModuleDependencies(ClassId.topLevel(commonType.typeFqName))!!
        val wideType = wideClass.defaultType
        val wideFunction = wideClass.unsubstitutedMemberScope
                .getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                .filterIsInstance<FunctionDescriptor>()
                .single { it.name == expression.descriptor.name && it.valueParameters.getOrNull(0)?.type == wideType }

        val call = IrCallImpl(
                expression.startOffset, expression.endOffset,
                IrSimpleFunctionSymbolImpl(wideFunction), wideFunction,
                origin = expression.origin)
        call.dispatchReceiver = firstArg
        call.putValueArgument(0, secondArg)

        return call
    }

    private fun KotlinType.widenTo(to: PrimitiveType, value: IrExpression, origin: IrStatementOrigin?): IrExpression {
        val fromClass = (constructor.declarationDescriptor as ClassDescriptor)
        val functionName = "to" + to.typeName.asString()
        val conversionFunction = fromClass.unsubstitutedMemberScope
                .getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                .filterIsInstance<FunctionDescriptor>()
                .single { it.name.asString() == functionName && it.valueParameters.isEmpty() }
        val call = IrCallImpl(
                value.startOffset, value.endOffset,
                IrSimpleFunctionSymbolImpl(conversionFunction), conversionFunction,
                origin = origin)
        call.dispatchReceiver = value
        return call
    }

    private fun PrimitiveType.canBeWidenedTo(that: PrimitiveType): Boolean = when (this) {
        PrimitiveType.BYTE -> when (that) {
            PrimitiveType.SHORT,
            PrimitiveType.INT,
            PrimitiveType.LONG,
            PrimitiveType.FLOAT,
            PrimitiveType.DOUBLE -> true
            else -> false
        }
        PrimitiveType.SHORT -> when (that) {
            PrimitiveType.INT,
            PrimitiveType.LONG,
            PrimitiveType.FLOAT,
            PrimitiveType.DOUBLE -> true
            else -> false
        }
        PrimitiveType.INT -> when (that) {
            PrimitiveType.LONG,
            PrimitiveType.FLOAT,
            PrimitiveType.DOUBLE -> true
            else -> false
        }
        PrimitiveType.LONG -> when (that) {
            PrimitiveType.FLOAT,
            PrimitiveType.DOUBLE -> true
            else -> false
        }
        PrimitiveType.FLOAT -> when (that) {
            PrimitiveType.DOUBLE -> true
            else -> false
        }
        else -> false
    }
}