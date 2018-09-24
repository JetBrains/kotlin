/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.ir.types.withHasQuestionMark
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.makeNullable

class IrBuiltIns(
    val builtIns: KotlinBuiltIns,
    private val typeTranslator: TypeTranslator,
    outerSymbolTable: SymbolTable? = null
) {
    val languageVersionSettings = typeTranslator.languageVersionSettings

    private val builtInsModule = builtIns.builtInsModule

    private val packageFragment = IrBuiltinsPackageFragmentDescriptorImpl(builtInsModule, KOTLIN_INTERNAL_IR_FQN)
    val irBuiltInsExternalPackageFragment = IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(packageFragment))

    private val symbolTable = outerSymbolTable ?: SymbolTable()
    private val stubBuilder =
        DeclarationStubGenerator(builtInsModule, symbolTable, IrDeclarationOrigin.IR_BUILTINS_STUB, languageVersionSettings)

    private fun ClassDescriptor.toIrSymbol() = symbolTable.referenceClass(this)
    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun defineOperator(name: String, returnType: KotlinType, valueParameterTypes: List<KotlinType>): IrSimpleFunction {
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
        irSimpleFunction.parent = irBuiltInsExternalPackageFragment
        return irSimpleFunction
    }

    private fun <T : SimpleFunctionDescriptor> T.addStub(): IrSimpleFunction =
        addStubToPackageFragment(this)

    private fun defineComparisonOperator(name: String, operandType: KotlinType) =
        defineOperator(name, bool, listOf(operandType, operandType))

    private fun List<SimpleType>.defineComparisonOperatorForEachType(name: String) =
        associate { it to defineComparisonOperator(name, it) }

    val any = builtIns.anyType
    val anyN = builtIns.nullableAnyType
    val anyType = any.toIrType()
    val anyClass = builtIns.any.toIrSymbol()
    val anyNType = anyType.withHasQuestionMark(true)

    val bool = builtIns.booleanType
    val booleanType = bool.toIrType()
    val booleanClass = builtIns.boolean.toIrSymbol()

    val char = builtIns.charType
    val charType = char.toIrType()
    val charClass = builtIns.char.toIrSymbol()

    val byte = builtIns.byteType
    val byteType = byte.toIrType()
    val byteClass = builtIns.byte.toIrSymbol()

    val short = builtIns.shortType
    val shortType = short.toIrType()
    val shortClass = builtIns.short.toIrSymbol()

    val int = builtIns.intType
    val intType = int.toIrType()
    val intClass = builtIns.int.toIrSymbol()

    val long = builtIns.longType
    val longType = long.toIrType()
    val longClass = builtIns.long.toIrSymbol()

    val float = builtIns.floatType
    val floatType = float.toIrType()
    val floatClass = builtIns.float.toIrSymbol()

    val double = builtIns.doubleType
    val doubleType = double.toIrType()
    val doubleClass = builtIns.double.toIrSymbol()

    val nothing = builtIns.nothingType
    val nothingN = builtIns.nullableNothingType
    val nothingType = nothing.toIrType()
    val nothingClass = builtIns.nothing.toIrSymbol()
    val nothingNType = nothingType.withHasQuestionMark(true)

    val unit = builtIns.unitType
    val unitType = unit.toIrType()
    val unitClass = builtIns.unit.toIrSymbol()

    val string = builtIns.stringType
    val stringType = string.toIrType()
    val stringClass = builtIns.string.toIrSymbol()

    val arrayClass = builtIns.array.toIrSymbol()

    val throwableType = builtIns.throwable.defaultType.toIrType()
    val throwableClass = builtIns.throwable.toIrSymbol()

    val kCallableClass = builtIns.getBuiltInClassByFqName(KotlinBuiltIns.FQ_NAMES.kCallable.toSafe()).toIrSymbol()
    val kPropertyClass = builtIns.getBuiltInClassByFqName(KotlinBuiltIns.FQ_NAMES.kProperty.asSingleFqName()).toIrSymbol()

    // TODO switch to IrType
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
    val throwCceFun = defineOperator("THROW_CCE", nothing, listOf())
    val throwIseFun = defineOperator("THROW_ISE", nothing, listOf())
    val booleanNotFun = defineOperator("NOT", bool, listOf(bool))
    val noWhenBranchMatchedExceptionFun = defineOperator("noWhenBranchMatchedException", nothing, listOf())
    val illegalArgumentExceptionFun = defineOperator("illegalArgumentException", nothing, listOf(string))

    val eqeqeq = eqeqeqFun.descriptor
    val eqeq = eqeqFun.descriptor
    val throwNpe = throwNpeFun.descriptor
    val throwCce = throwCceFun.descriptor
    val booleanNot = booleanNotFun.descriptor
    val noWhenBranchMatchedException = noWhenBranchMatchedExceptionFun.descriptor
    val illegalArgumentException = illegalArgumentExceptionFun.descriptor

    val eqeqeqSymbol = eqeqeqFun.symbol
    val eqeqSymbol = eqeqFun.symbol
    val throwNpeSymbol = throwNpeFun.symbol
    val throwCceSymbol = throwCceFun.symbol
    val throwIseSymbol = throwIseFun.symbol
    val booleanNotSymbol = booleanNotFun.symbol
    val noWhenBranchMatchedExceptionSymbol = noWhenBranchMatchedExceptionFun.symbol
    val illegalArgumentExceptionSymbol = illegalArgumentExceptionFun.symbol

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
