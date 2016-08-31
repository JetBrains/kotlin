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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitution

class IrBuiltIns(val builtIns: KotlinBuiltIns) {
    val packageFragment = IrBuiltinsPackageFragmentDescriptorImpl(builtIns.builtInsModule)

    val eqeqeq: FunctionDescriptor
    val eqeq: FunctionDescriptor
    val lt0: FunctionDescriptor
    val lteq0: FunctionDescriptor
    val gt0: FunctionDescriptor
    val gteq0: FunctionDescriptor
    val notNull: FunctionDescriptor
    val throwNpe: FunctionDescriptor

    val booleanNot: FunctionDescriptor

    val implicitNotNull: CallableDescriptor // TODO drop

    init {
        val bool = builtIns.booleanType
        val any = builtIns.anyType
        val anyN = builtIns.nullableAnyType
        val int = builtIns.intType
        val nothing = builtIns.nothingType

        eqeqeq = defineOperator("EQEQEQ", bool, listOf(anyN, anyN))
        eqeq = defineOperator("EQEQ", bool, listOf(anyN, anyN))
        lt0 = defineOperator("LT0", bool, listOf(int))
        lteq0 = defineOperator("LTEQ0", bool, listOf(int))
        gt0 = defineOperator("GT0", bool, listOf(int))
        gteq0 = defineOperator("GTEQ0", bool, listOf(int))
        notNull = defineOperator("NOT_NULL", bool, listOf(anyN))
        throwNpe = defineOperator("THROW_NPE", nothing, listOf())

        implicitNotNull = defineOperator("IMPLICIT_NOT_NULL", any, listOf(anyN))

        // booleanNot = builtIns.boolean.findSingleFunction("not") // TODO requires receiver
        booleanNot = defineOperator("NOT", bool, listOf(bool))
    }

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
