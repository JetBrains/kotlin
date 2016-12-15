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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.Variance

class BuiltinsOperatorsBuilder(val packageFragment: PackageFragmentDescriptor, val builtIns: KotlinBuiltIns) {
    val bool = builtIns.booleanType
    val any = builtIns.anyType
    val anyN = builtIns.nullableAnyType
    val int = builtIns.intType
    val nothing = builtIns.nothingType
    val unit = builtIns.unitType

    fun defineOperator(name: String, returnType: KotlinType, valueParameterTypes: List<KotlinType>): IrBuiltinOperatorDescriptor {
        val operatorDescriptor = IrSimpleBuiltinOperatorDescriptorImpl(packageFragment, Name.identifier(name), returnType)
        for ((i, valueParameterType) in valueParameterTypes.withIndex()) {
            operatorDescriptor.addValueParameter(
                    IrBuiltinValueParameterDescriptorImpl(operatorDescriptor, Name.identifier("arg$i"), i, valueParameterType))
        }
        return operatorDescriptor
    }
}

class IrBuiltIns(val builtIns: KotlinBuiltIns) {
    private val packageFragment = IrBuiltinsPackageFragmentDescriptorImpl(builtIns.builtInsModule, KOTLIN_INTERNAL_IR_FQN)
    private val builder = BuiltinsOperatorsBuilder(packageFragment, builtIns)

    val eqeqeq: FunctionDescriptor = builder.run { defineOperator("EQEQEQ", bool, listOf(anyN, anyN)) }
    val eqeq: FunctionDescriptor = builder.run { defineOperator("EQEQ", bool, listOf(anyN, anyN)) }
    val lt0: FunctionDescriptor = builder.run { defineOperator("LT0", bool, listOf(int)) }
    val lteq0: FunctionDescriptor = builder.run { defineOperator("LTEQ0", bool, listOf(int)) }
    val gt0: FunctionDescriptor = builder.run { defineOperator("GT0", bool, listOf(int)) }
    val gteq0: FunctionDescriptor = builder.run { defineOperator("GTEQ0", bool, listOf(int)) }
    val throwNpe: FunctionDescriptor = builder.run { defineOperator("THROW_NPE", nothing, listOf()) }
    val booleanNot: FunctionDescriptor = builder.run { defineOperator("NOT", bool, listOf(bool)) }

    val noWhenBranchMatchedException: FunctionDescriptor = builder.run { defineOperator("noWhenBranchMatchedException", unit, listOf()) }

    val enumValueOf: FunctionDescriptor =
            SimpleFunctionDescriptorImpl.create(
                    packageFragment,
                    Annotations.EMPTY,
                    Name.identifier("enumValueOf"),
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    SourceElement.NO_SOURCE
            ).apply {
                val typeParameterT = TypeParameterDescriptorImpl.createWithDefaultBound(
                        this, Annotations.EMPTY, true, Variance.INVARIANT, Name.identifier("T"), 0
                )

                val valueParameterName = ValueParameterDescriptorImpl(
                        this, null, 0, Annotations.EMPTY, Name.identifier("name"), builtIns.stringType,
                        false, false, false, null, org.jetbrains.kotlin.descriptors.SourceElement.NO_SOURCE
                )

                val returnType = KotlinTypeFactory.simpleType(Annotations.EMPTY, typeParameterT.typeConstructor, listOf(), false)

                initialize(null, null, listOf(typeParameterT), listOf(valueParameterName), returnType, Modality.FINAL, Visibilities.PUBLIC)
            }

    companion object {
        val KOTLIN_INTERNAL_IR_FQN = FqName("kotlin.internal.ir")
    }
}
