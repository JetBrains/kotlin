/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.org.objectweb.asm.Type

class IrIntrinsicMethods(irBuiltIns: IrBuiltIns) {

    val intrinsics = IntrinsicMethods()

    private val irMapping = hashMapOf<CallableMemberDescriptor, IntrinsicMethod>()

    private fun createPrimitiveComparisonIntrinsics(typeToIrFun: Map<SimpleType, IrSimpleFunction>, operator: KtSingleValueToken) {
        for ((type, irFun) in typeToIrFun) {
            irMapping[irFun.descriptor] = PrimitiveComparison(type, operator)
        }
    }

    init {
        irMapping[irBuiltIns.eqeq] = Equals(KtTokens.EQEQ)
        irMapping[irBuiltIns.eqeqeq] = Equals(KtTokens.EQEQEQ)
        irMapping[irBuiltIns.ieee754equalsFunByOperandType[irBuiltIns.float]!!.descriptor] = Ieee754Equals(Type.FLOAT_TYPE)
        irMapping[irBuiltIns.ieee754equalsFunByOperandType[irBuiltIns.double]!!.descriptor] = Ieee754Equals(Type.DOUBLE_TYPE)
        irMapping[irBuiltIns.booleanNot] = Not()

        createPrimitiveComparisonIntrinsics(irBuiltIns.lessFunByOperandType, KtTokens.LT)
        createPrimitiveComparisonIntrinsics(irBuiltIns.lessOrEqualFunByOperandType, KtTokens.LTEQ)
        createPrimitiveComparisonIntrinsics(irBuiltIns.greaterFunByOperandType, KtTokens.GT)
        createPrimitiveComparisonIntrinsics(irBuiltIns.greaterOrEqualFunByOperandType, KtTokens.GTEQ)

        irMapping[irBuiltIns.enumValueOf] = IrEnumValueOf()
        irMapping[irBuiltIns.noWhenBranchMatchedException] = IrNoWhenBranchMatchedException()
        irMapping[irBuiltIns.illegalArgumentException] = IrIllegalArgumentException()
        irMapping[irBuiltIns.throwNpe] = ThrowNPE()
    }

    fun getIntrinsic(descriptor: CallableMemberDescriptor): IntrinsicMethod? {
        intrinsics.getIntrinsic(descriptor)?.let { return it }
        if (descriptor is PropertyAccessorDescriptor) {
            return intrinsics.getIntrinsic(DescriptorUtils.unwrapFakeOverride(descriptor.correspondingProperty))
        }
        return irMapping[descriptor.original]
    }
}
