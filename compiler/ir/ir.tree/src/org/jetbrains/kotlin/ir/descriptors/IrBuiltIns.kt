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

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class IrBuiltIns(val builtIns: KotlinBuiltIns) {
    val packageFragment = IrBuiltinsPackageFragmentDescriptorImpl(builtIns.builtInsModule)

    val eqeqeq: IrBuiltinOperatorDescriptor
    val eqeq: IrBuiltinOperatorDescriptor
    val lt0: IrBuiltinOperatorDescriptor
    val lteq0: IrBuiltinOperatorDescriptor
    val gt0: IrBuiltinOperatorDescriptor
    val gteq0: IrBuiltinOperatorDescriptor

    init {
        val bool = builtIns.booleanType
        val anyN = builtIns.nullableAnyType
        val int = builtIns.intType

        eqeqeq = defineOperator("EQEQEQ", bool, listOf(anyN, anyN))
        eqeq = defineOperator("EQEQ", bool, listOf(anyN, anyN))
        lt0 = defineOperator("LT0", bool, listOf(int))
        lteq0 = defineOperator("LTEQ0", bool, listOf(int))
        gt0 = defineOperator("GT0", bool, listOf(int))
        gteq0 = defineOperator("GTEQ0", bool, listOf(int))
    }

    private fun defineOperator(name: String, returnType: KotlinType, valueParameterTypes: List<KotlinType>): IrBuiltinOperatorDescriptor {
        val operatorDescriptor = IrSimpleBuiltinOperatorDescriptorImpl(packageFragment, Name.identifier(name), returnType)
        for ((i, valueParameterType) in valueParameterTypes.withIndex()) {
            operatorDescriptor.addValueParameter(
                    IrBuiltinValueParameterDescriptorImpl(operatorDescriptor, Name.identifier("arg$i"), i, valueParameterType))
        }
        return operatorDescriptor
    }
}
