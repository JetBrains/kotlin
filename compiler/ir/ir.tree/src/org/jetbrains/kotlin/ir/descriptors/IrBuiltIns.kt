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
import org.jetbrains.kotlin.types.Variance

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

    val bool = builtIns.booleanType
    val any = builtIns.anyType
    val anyN = builtIns.nullableAnyType
    val int = builtIns.intType
    val nothing = builtIns.nothingType
    val unit = builtIns.unitType

    val eqeqeqFun = defineOperator("EQEQEQ", bool, listOf(anyN, anyN))
    val eqeqFun = defineOperator("EQEQ", bool, listOf(anyN, anyN))
    val lt0Fun = defineOperator("LT0", bool, listOf(int))
    val lteq0Fun = defineOperator("LTEQ0", bool, listOf(int))
    val gt0Fun = defineOperator("GT0", bool, listOf(int))
    val gteq0Fun = defineOperator("GTEQ0", bool, listOf(int))
    val throwNpeFun = defineOperator("THROW_NPE", nothing, listOf())
    val booleanNotFun = defineOperator("NOT", bool, listOf(bool))
    val noWhenBranchMatchedExceptionFun = defineOperator("noWhenBranchMatchedException", unit, listOf())

    val eqeqeq = eqeqeqFun.descriptor
    val eqeq = eqeqFun.descriptor
    val lt0 = lt0Fun.descriptor
    val lteq0 = lteq0Fun.descriptor
    val gt0 = gt0Fun.descriptor
    val gteq0 = gteq0Fun.descriptor
    val throwNpe = throwNpeFun.descriptor
    val booleanNot = booleanNotFun.descriptor
    val noWhenBranchMatchedException = noWhenBranchMatchedExceptionFun.descriptor

    val eqeqeqSymbol = eqeqeqFun.symbol
    val eqeqSymbol = eqeqFun.symbol
    val lt0Symbol = lt0Fun.symbol
    val lteq0Symbol = lteq0Fun.symbol
    val gt0Symbol = gt0Fun.symbol
    val gteq0Symbol = gteq0Fun.symbol
    val throwNpeSymbol = throwNpeFun.symbol
    val booleanNotSymbol = booleanNotFun.symbol
    val noWhenBranchMatchedExceptionSymbol = noWhenBranchMatchedExceptionFun.symbol

    val enumValueOfFun =
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
            }.addStub()
    val enumValueOf = enumValueOfFun.descriptor
    val enumValueOfSymbol = enumValueOfFun.symbol

    companion object {
        val KOTLIN_INTERNAL_IR_FQN = FqName("kotlin.internal.ir")
    }
}
