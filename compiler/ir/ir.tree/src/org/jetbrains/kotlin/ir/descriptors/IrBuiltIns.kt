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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitution

class IrBuiltIns(val builtIns: KotlinBuiltIns) {
    val packageFragment = IrBuiltinsPackageFragmentDescriptorImpl(builtIns.builtInsModule)

    val bool = builtIns.booleanType
    val any = builtIns.anyType
    val anyN = builtIns.nullableAnyType
    val int = builtIns.intType
    val nothing = builtIns.nothingType

    val eqeqeq: FunctionDescriptor = defineOperator("EQEQEQ", bool, listOf(anyN, anyN))
    val eqeq: FunctionDescriptor = defineOperator("EQEQ", bool, listOf(anyN, anyN))
    val lt0: FunctionDescriptor = defineOperator("LT0", bool, listOf(int))
    val lteq0: FunctionDescriptor = defineOperator("LTEQ0", bool, listOf(int))
    val gt0: FunctionDescriptor = defineOperator("GT0", bool, listOf(int))
    val gteq0: FunctionDescriptor = defineOperator("GTEQ0", bool, listOf(int))
    val throwNpe: FunctionDescriptor = defineOperator("THROW_NPE", nothing, listOf())
    val booleanNot: FunctionDescriptor = defineOperator("NOT", bool, listOf(bool))

    private fun defineOperator(name: String, returnType: KotlinType, valueParameterTypes: List<KotlinType>): IrBuiltinOperatorDescriptor {
        val operatorDescriptor = IrSimpleBuiltinOperatorDescriptorImpl(packageFragment, Name.identifier(name), returnType)
        for ((i, valueParameterType) in valueParameterTypes.withIndex()) {
            operatorDescriptor.addValueParameter(
                    IrBuiltinValueParameterDescriptorImpl(operatorDescriptor, Name.identifier("arg$i"), i, valueParameterType))
        }
        return operatorDescriptor
    }

    private fun ClassDescriptor.findSingleFunction(name: String): FunctionDescriptor =
            getMemberScope(TypeSubstitution.EMPTY).getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BUILTINS).single()
}
