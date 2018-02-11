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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.makeNullable

class IrBuiltIns(val builtIns: KotlinBuiltIns) {
    private val packageFragment = IrBuiltinsPackageFragmentDescriptorImpl(builtIns.builtInsModule, KOTLIN_INTERNAL_IR_FQN)
    val irBuiltInsExternalPackageFragment = IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(packageFragment))

    private val stubBuilder = DeclarationStubGenerator(SymbolTable(), IrDeclarationOrigin.IR_BUILTINS_STUB)

    private fun defineOperator(name: String, returnType: KotlinType, valueParameterTypes: List<KotlinType>): IrSimpleFunction {
        val operatorDescriptor = IrSimpleBuiltinOperatorDescriptorImpl(packageFragment, Name.identifier(name), returnType)
        for ((i, valueParameterType) in valueParameterTypes.withIndex()) {
            operatorDescriptor.addValueParameter(
                IrBuiltinValueParameterDescriptorImpl(operatorDescriptor, Name.identifier("arg$i"), i, valueParameterType)
            )
        }
        return addStubToPackageFragment(operatorDescriptor)
    }

    private fun addStubToPackageFragment(descriptor: SimpleFunctionDescriptor): IrSimpleFunction {
        val irSimpleFunction = stubBuilder.generateFunctionStub(descriptor)
        irBuiltInsExternalPackageFragment.declarations.add(irSimpleFunction)
        return irSimpleFunction
    }

    private fun <T : SimpleFunctionDescriptor> T.addStub(): IrSimpleFunction =
        addStubToPackageFragment(this)

    private fun defineComparisonOperator(name: String, operandType: KotlinType) =
        defineOperator(name, bool, listOf(operandType, operandType))

    private fun List<SimpleType>.defineComparisonOperatorForEachType(name: String) =
        associate { it to defineComparisonOperator(name, it) }

    val bool = builtIns.booleanType
    val any = builtIns.anyType
    val anyN = builtIns.nullableAnyType
    val char = builtIns.charType
    val byte = builtIns.byteType
    val short = builtIns.shortType
    val int = builtIns.intType
    val long = builtIns.longType
    val float = builtIns.floatType
    val double = builtIns.doubleType
    val nothing = builtIns.nothingType
    val unit = builtIns.unitType
    val string = builtIns.stringType

    val primitiveTypes = listOf(bool, char, byte, short, int, long, float, double)
    val primitiveTypesWithComparisons = listOf(int, long, float, double)
    val primitiveFloatingPointTypes = listOf(float, double)

    val lessFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType("less")
    val lessOrEqualFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType("lessOrEqual")
    val greaterOrEqualFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType("greaterOrEqual")
    val greaterFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType("greater")

    val ieee754equalsFunByOperandType =
        primitiveFloatingPointTypes.associate {
            it to defineOperator("ieee754equals", bool, listOf(it.makeNullable(), it.makeNullable()))
        }

    val eqeqeqFun = defineOperator("EQEQEQ", bool, listOf(anyN, anyN))
    val eqeqFun = defineOperator("EQEQ", bool, listOf(anyN, anyN))
    val throwNpeFun = defineOperator("THROW_NPE", nothing, listOf())
    val booleanNotFun = defineOperator("NOT", bool, listOf(bool))
    val noWhenBranchMatchedExceptionFun = defineOperator("noWhenBranchMatchedException", unit, listOf())

    val eqeqeq = eqeqeqFun.descriptor
    val eqeq = eqeqFun.descriptor
    val throwNpe = throwNpeFun.descriptor
    val booleanNot = booleanNotFun.descriptor
    val noWhenBranchMatchedException = noWhenBranchMatchedExceptionFun.descriptor

    val eqeqeqSymbol = eqeqeqFun.symbol
    val eqeqSymbol = eqeqFun.symbol
    val throwNpeSymbol = throwNpeFun.symbol
    val booleanNotSymbol = booleanNotFun.symbol
    val noWhenBranchMatchedExceptionSymbol = noWhenBranchMatchedExceptionFun.symbol

    val enumValueOfFun = createEnumValueOfFun()
    val enumValueOf = enumValueOfFun.descriptor
    val enumValueOfSymbol = enumValueOfFun.symbol

    private fun createEnumValueOfFun(): IrSimpleFunction =
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
                false, false, false, null, SourceElement.NO_SOURCE
            )

            val returnType = KotlinTypeFactory.simpleType(Annotations.EMPTY, typeParameterT.typeConstructor, listOf(), false)

            initialize(null, null, listOf(typeParameterT), listOf(valueParameterName), returnType, Modality.FINAL, Visibilities.PUBLIC)
        }.addStub()

    val dataClassArrayMemberHashCodeFun = defineOperator("dataClassArrayMemberHashCode", int, listOf(any))
    val dataClassArrayMemberHashCode = dataClassArrayMemberHashCodeFun.descriptor
    val dataClassArrayMemberHashCodeSymbol = dataClassArrayMemberHashCodeFun.symbol

    val dataClassArrayMemberToStringFun = defineOperator("dataClassArrayMemberToString", string, listOf(anyN))
    val dataClassArrayMemberToString = dataClassArrayMemberToStringFun.descriptor
    val dataClassArrayMemberToStringSymbol = dataClassArrayMemberToStringFun.symbol

    companion object {
        val KOTLIN_INTERNAL_IR_FQN = FqName("kotlin.internal.ir")
    }
}
