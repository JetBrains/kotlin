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
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
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
    private val stubBuilder = DeclarationStubGenerator(
        builtInsModule, symbolTable, languageVersionSettings, externalDeclarationOrigin = { IrDeclarationOrigin.IR_BUILTINS_STUB }
    )

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

    private class IrTypeMapper(val type: () -> IrType, val nType: () -> IrType)

    private fun buildNullableType(irType: IrType) = with(irType as IrSimpleType) {
        IrSimpleTypeImpl(classifier, true, arguments, annotations)
    }

    private val primitiveTypesLazyMapping = mapOf<ClassifierDescriptor, IrTypeMapper>(
        builtIns.any to IrTypeMapper({ anyType }, { anyNType }),
        builtIns.boolean to IrTypeMapper({ booleanType }, { buildNullableType(booleanType) }),
        builtIns.char to IrTypeMapper({ charType }, { buildNullableType(charType) }),
        builtIns.number to IrTypeMapper({ numberType }, { buildNullableType(numberType) }),
        builtIns.byte to IrTypeMapper({ byteType }, { buildNullableType(byteType) }),
        builtIns.short to IrTypeMapper({ shortType }, { buildNullableType(shortType) }),
        builtIns.int to IrTypeMapper({ intType }, { buildNullableType(intType) }),
        builtIns.long to IrTypeMapper({ longType }, { buildNullableType(longType) }),
        builtIns.float to IrTypeMapper({ floatType }, { buildNullableType(floatType) }),
        builtIns.double to IrTypeMapper({ doubleType }, { buildNullableType(doubleType) }),
        builtIns.nothing to IrTypeMapper({ nothingType }, { nothingNType }),
        builtIns.unit to IrTypeMapper({ unitType }, { buildNullableType(unitType) }),
        builtIns.string to IrTypeMapper({ stringType }, { buildNullableType(stringType) }),
        builtIns.throwable to IrTypeMapper({ throwableType }, { buildNullableType(throwableType) })//,
//        builtIns.array to IrTypeMapper({ arrayType }, { buildNullableType(arrayType) })
    )

    fun getPrimitiveTypeOrNullByDescriptor(descriptor: ClassifierDescriptor, isNullable: Boolean) =
        primitiveTypesLazyMapping[descriptor]?.let {
            if (isNullable) it.nType() else it.type()
        } as IrSimpleType?

    val any = builtIns.anyType
    val anyN = builtIns.nullableAnyType
    val anyType by lazy { any.toIrType() }
    val anyClass by lazy { builtIns.any.toIrSymbol() }
    val anyNType by lazy { anyType.withHasQuestionMark(true) }

    val bool = builtIns.booleanType
    val booleanType by lazy { bool.toIrType() }
    val booleanClass by lazy { builtIns.boolean.toIrSymbol() }

    val char = builtIns.charType
    val charType by lazy { char.toIrType() }
    val charClass by lazy { builtIns.char.toIrSymbol() }

    val number = builtIns.number.defaultType
    val numberType by lazy { number.toIrType() }
    val numberClass by lazy { builtIns.number.toIrSymbol() }

    val byte = builtIns.byteType
    val byteType by lazy { byte.toIrType() }
    val byteClass by lazy { builtIns.byte.toIrSymbol() }

    val short = builtIns.shortType
    val shortType by lazy { short.toIrType() }
    val shortClass by lazy { builtIns.short.toIrSymbol() }

    val int = builtIns.intType
    val intType by lazy { int.toIrType() }
    val intClass by lazy { builtIns.int.toIrSymbol() }

    val long = builtIns.longType
    val longType by lazy { long.toIrType() }
    val longClass by lazy { builtIns.long.toIrSymbol() }

    val float = builtIns.floatType
    val floatType by lazy { float.toIrType() }
    val floatClass by lazy { builtIns.float.toIrSymbol() }

    val double = builtIns.doubleType
    val doubleType by lazy { double.toIrType() }
    val doubleClass by lazy { builtIns.double.toIrSymbol() }

    val nothing = builtIns.nothingType
    val nothingN = builtIns.nullableNothingType
    val nothingType by lazy { nothing.toIrType() }
    val nothingClass by lazy { builtIns.nothing.toIrSymbol() }
    val nothingNType by lazy { nothingType.withHasQuestionMark(true) }

    val unit = builtIns.unitType
    val unitType by lazy { unit.toIrType() }
    val unitClass by lazy { builtIns.unit.toIrSymbol() }

    val string = builtIns.stringType
    val stringType by lazy { string.toIrType() }
    val stringClass by lazy { builtIns.string.toIrSymbol() }

    val collectionClass = builtIns.collection.toIrSymbol()

    val arrayType by lazy { builtIns.array.toIrType(symbolTable = symbolTable) }
    val arrayClass by lazy { builtIns.array.toIrSymbol() }

    val throwableType by lazy { builtIns.throwable.defaultType.toIrType() }
    val throwableClass by lazy { builtIns.throwable.toIrSymbol() }

    val primitiveIrTypes by lazy { listOf(booleanType, charType, byteType, shortType, intType, floatType, longType, doubleType) }

    val kCallableClass = builtIns.getBuiltInClassByFqName(KotlinBuiltIns.FQ_NAMES.kCallable.toSafe()).toIrSymbol()
    val kPropertyClass = builtIns.getBuiltInClassByFqName(KotlinBuiltIns.FQ_NAMES.kPropertyFqName.toSafe()).toIrSymbol()

    // TODO switch to IrType
    val primitiveTypes = listOf(bool, char, byte, short, int, long, float, double)
    val primitiveTypesWithComparisons = listOf(int, long, float, double)
    val primitiveFloatingPointTypes = listOf(float, double)

    val lessFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType(OperatorNames.LESS)
    val lessOrEqualFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType(OperatorNames.LESS_OR_EQUAL)
    val greaterOrEqualFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType(OperatorNames.GREATER_OR_EQUAL)
    val greaterFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType(OperatorNames.GREATER)

    val ieee754equalsFunByOperandType =
        primitiveFloatingPointTypes.associate {
            it to defineOperator(OperatorNames.IEEE754_EQUALS, bool, listOf(it.makeNullable(), it.makeNullable()))
        }

    val eqeqeqFun = defineOperator(OperatorNames.EQEQEQ, bool, listOf(anyN, anyN))
    val eqeqFun = defineOperator(OperatorNames.EQEQ, bool, listOf(anyN, anyN))
    val throwNpeFun = defineOperator(OperatorNames.THROW_NPE, nothing, listOf())
    val throwCceFun = defineOperator(OperatorNames.THROW_CCE, nothing, listOf())
    val throwIseFun = defineOperator(OperatorNames.THROW_ISE, nothing, listOf())
    val booleanNotFun = defineOperator(OperatorNames.NOT, bool, listOf(bool))
    val noWhenBranchMatchedExceptionFun = defineOperator(OperatorNames.NO_WHEN_BRANCH_MATCHED_EXCEPTION, nothing, listOf())
    val illegalArgumentExceptionFun = defineOperator(OperatorNames.ILLEGAL_ARGUMENT_EXCEPTION, nothing, listOf(string))

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

    object OperatorNames {
        const val LESS = "less"
        const val LESS_OR_EQUAL = "lessOrEqual"
        const val GREATER = "greater"
        const val GREATER_OR_EQUAL = "greaterOrEqual"
        const val EQEQ = "EQEQ"
        const val EQEQEQ = "EQEQEQ"
        const val IEEE754_EQUALS = "ieee754equals"
        const val NOT = "NOT"
        const val THROW_NPE = "THROW_NPE"
        const val THROW_CCE = "THROW_CCE"
        const val THROW_ISE = "THROW_ISE"
        const val NO_WHEN_BRANCH_MATCHED_EXCEPTION = "noWhenBranchMatchedException"
        const val ILLEGAL_ARGUMENT_EXCEPTION = "illegalArgumentException "
    }
}
