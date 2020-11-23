/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeBuilder
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.withHasQuestionMark
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*

class IrBuiltIns(
    val builtIns: KotlinBuiltIns,
    private val typeTranslator: TypeTranslator,
    private val symbolTable: SymbolTable
) {
    val languageVersionSettings = typeTranslator.languageVersionSettings

    lateinit var functionFactory: IrAbstractFunctionFactory
    val irFactory: IrFactory = symbolTable.irFactory

    private val builtInsModule = builtIns.builtInsModule

    private val packageFragmentDescriptor = IrBuiltinsPackageFragmentDescriptorImpl(builtInsModule, KOTLIN_INTERNAL_IR_FQN)
    val packageFragment =
        IrExternalPackageFragmentImpl(symbolTable.referenceExternalPackageFragment(packageFragmentDescriptor), KOTLIN_INTERNAL_IR_FQN)

    private fun ClassDescriptor.toIrSymbol() = symbolTable.referenceClass(this)
    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun defineOperator(name: String, returnType: IrType, valueParameterTypes: List<IrType>): IrSimpleFunctionSymbol {
        val operatorDescriptor =
            IrSimpleBuiltinOperatorDescriptorImpl(packageFragmentDescriptor, Name.identifier(name), returnType.originalKotlinType!!)

        for ((i, valueParameterType) in valueParameterTypes.withIndex()) {
            operatorDescriptor.addValueParameter(
                IrBuiltinValueParameterDescriptorImpl(
                    operatorDescriptor, Name.identifier("arg$i"), i, valueParameterType.originalKotlinType!!
                )
            )
        }

        val symbol = symbolTable.declareSimpleFunctionIfNotExists(operatorDescriptor) {
            val operator = irFactory.createFunction(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, it, Name.identifier(name), DescriptorVisibilities.PUBLIC, Modality.FINAL,
                returnType, isInline = false, isExternal = false, isTailrec = false, isSuspend = false,
                isOperator = false, isInfix = false, isExpect = false, isFakeOverride = false
            )
            operator.parent = packageFragment
            packageFragment.declarations += operator

            operator.valueParameters = valueParameterTypes.withIndex().map { (i, valueParameterType) ->
                val valueParameterDescriptor = operatorDescriptor.valueParameters[i]
                val valueParameterSymbol = IrValueParameterSymbolImpl(valueParameterDescriptor)
                irFactory.createValueParameter(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, valueParameterSymbol, Name.identifier("arg$i"), i,
                    valueParameterType, null, isCrossinline = false, isNoinline = false, isAssignable = false
                ).apply {
                    parent = operator
                }
            }

            operator
        }

        return symbol.symbol
    }

    private fun defineCheckNotNullOperator(): IrSimpleFunctionSymbol {
        val name = Name.identifier("CHECK_NOT_NULL")
        val typeParameterDescriptor: TypeParameterDescriptor
        val valueParameterDescriptor: ValueParameterDescriptor

        val returnKotlinType: SimpleType
        val valueKotlinType: SimpleType

        // Note: We still need a complete function descriptor here because `CHECK_NOT_NULL` is being substituted by psi2ir
        val operatorDescriptor = SimpleFunctionDescriptorImpl.create(
            packageFragmentDescriptor,
            Annotations.EMPTY,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        ).apply {
            typeParameterDescriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                this, Annotations.EMPTY, false, Variance.INVARIANT, Name.identifier("T0"),
                0, SourceElement.NO_SOURCE, LockBasedStorageManager.NO_LOCKS
            ).apply {
                addUpperBound(any)
                setInitialized()
            }

            valueKotlinType = typeParameterDescriptor.typeConstructor.makeNullableType()

            valueParameterDescriptor = ValueParameterDescriptorImpl(
                this, null, 0, Annotations.EMPTY, Name.identifier("arg0"), valueKotlinType,
                declaresDefaultValue = false, isCrossinline = false, isNoinline = false, varargElementType = null,
                source = SourceElement.NO_SOURCE
            )

            returnKotlinType = typeParameterDescriptor.typeConstructor.makeNonNullType()

            initialize(
                null, null, listOf(typeParameterDescriptor), listOf(valueParameterDescriptor), returnKotlinType,
                Modality.FINAL, DescriptorVisibilities.PUBLIC
            )
        }

        val typeParameterSymbol = IrTypeParameterSymbolImpl(typeParameterDescriptor)
        val typeParameter = irFactory.createTypeParameter(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, typeParameterSymbol, Name.identifier("T0"), 0, true, Variance.INVARIANT
        ).apply {
            superTypes += anyType
        }

        val returnIrType = IrSimpleTypeBuilder().run {
            classifier = typeParameterSymbol
            kotlinType = returnKotlinType
            hasQuestionMark = false
            buildSimpleType()
        }

        val valueIrType = IrSimpleTypeBuilder().run {
            classifier = typeParameterSymbol
            kotlinType = valueKotlinType
            hasQuestionMark = true
            buildSimpleType()
        }

        return symbolTable.declareSimpleFunctionIfNotExists(operatorDescriptor) {
            val operator = irFactory.createFunction(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, it, name, DescriptorVisibilities.PUBLIC, Modality.FINAL, returnIrType,
                isInline = false, isExternal = false, isTailrec = false, isSuspend = false, isOperator = false, isInfix = false,
                isExpect = false, isFakeOverride = false
            )
            operator.parent = packageFragment
            packageFragment.declarations += operator

            val valueParameterSymbol = IrValueParameterSymbolImpl(valueParameterDescriptor)
            val valueParameter = irFactory.createValueParameter(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, valueParameterSymbol, Name.identifier("arg0"), 0,
                valueIrType, null, isCrossinline = false, isNoinline = false, isAssignable = false
            )

            valueParameter.parent = operator
            typeParameter.parent = operator

            operator.valueParameters += valueParameter
            operator.typeParameters += typeParameter

            operator
        }.symbol
    }

    private fun defineComparisonOperator(name: String, operandType: IrType) =
        defineOperator(name, booleanType, listOf(operandType, operandType))

    private fun List<IrType>.defineComparisonOperatorForEachIrType(name: String) =
        associate { it.classifierOrFail to defineComparisonOperator(name, it) }

    val any = builtIns.anyType
    val anyType = any.toIrType()
    val anyClass = builtIns.any.toIrSymbol()
    val anyNType = anyType.withHasQuestionMark(true)

    val bool = builtIns.booleanType
    val booleanType = bool.toIrType()
    val booleanClass = builtIns.boolean.toIrSymbol()

    val char = builtIns.charType
    val charType = char.toIrType()
    val charClass = builtIns.char.toIrSymbol()

    val number = builtIns.number.defaultType
    val numberType = number.toIrType()
    val numberClass = builtIns.number.toIrSymbol()

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
    val nothingType = nothing.toIrType()
    val nothingClass = builtIns.nothing.toIrSymbol()
    val nothingNType = nothingType.withHasQuestionMark(true)

    val unit = builtIns.unitType
    val unitType = unit.toIrType()
    val unitClass = builtIns.unit.toIrSymbol()

    val string = builtIns.stringType
    val stringType = string.toIrType()
    val stringClass = builtIns.string.toIrSymbol()

    val collectionClass = builtIns.collection.toIrSymbol()

    val arrayClass = builtIns.array.toIrSymbol()

    val throwableType = builtIns.throwable.defaultType.toIrType()
    val throwableClass = builtIns.throwable.toIrSymbol()

    val kCallableClass = builtIns.kCallable.toIrSymbol()
    val kPropertyClass = builtIns.kProperty.toIrSymbol()
    val kClassClass = builtIns.kClass.toIrSymbol()

    private val kProperty0Class = builtIns.kProperty0.toIrSymbol()
    private val kProperty1Class = builtIns.kProperty1.toIrSymbol()
    private val kProperty2Class = builtIns.kProperty2.toIrSymbol()
    private val kMutableProperty0Class = builtIns.kMutableProperty0.toIrSymbol()
    private val kMutableProperty1Class = builtIns.kMutableProperty1.toIrSymbol()
    private val kMutableProperty2Class = builtIns.kMutableProperty2.toIrSymbol()

    val functionClass = builtIns.getBuiltInClassByFqName(FqName("kotlin.Function")).toIrSymbol()
    val kFunctionClass = builtIns.getBuiltInClassByFqName(FqName("kotlin.reflect.KFunction")).toIrSymbol()

    fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol = when (n) {
        0 -> if (mutable) kMutableProperty0Class else kProperty0Class
        1 -> if (mutable) kMutableProperty1Class else kProperty1Class
        2 -> if (mutable) kMutableProperty2Class else kProperty2Class
        else -> error("No KProperty for n=$n mutable=$mutable")
    }

    // TODO switch to IrType
    val primitiveTypes = listOf(bool, char, byte, short, int, float, long, double)
    val primitiveIrTypes = listOf(booleanType, charType, byteType, shortType, intType, floatType, longType, doubleType)
    private val primitiveIrTypesWithComparisons = listOf(charType, byteType, shortType, intType, floatType, longType, doubleType)
    private val primitiveFloatingPointIrTypes = listOf(floatType, doubleType)
    val primitiveArrays = PrimitiveType.values().map { builtIns.getPrimitiveArrayClassDescriptor(it).toIrSymbol() }
    val primitiveArrayElementTypes = primitiveArrays.zip(primitiveIrTypes).toMap()
    val primitiveArrayForType = primitiveArrayElementTypes.asSequence().associate { it.value to it.key }

    val primitiveTypeToIrType = mapOf(
        PrimitiveType.BOOLEAN to booleanType,
        PrimitiveType.CHAR to charType,
        PrimitiveType.BYTE to byteType,
        PrimitiveType.SHORT to shortType,
        PrimitiveType.INT to intType,
        PrimitiveType.FLOAT to floatType,
        PrimitiveType.LONG to longType,
        PrimitiveType.DOUBLE to doubleType
    )

    val lessFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(OperatorNames.LESS)
    val lessOrEqualFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(OperatorNames.LESS_OR_EQUAL)
    val greaterOrEqualFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(OperatorNames.GREATER_OR_EQUAL)
    val greaterFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(OperatorNames.GREATER)

    val ieee754equalsFunByOperandType =
        primitiveFloatingPointIrTypes.map {
            it.classifierOrFail to defineOperator(OperatorNames.IEEE754_EQUALS, booleanType, listOf(it.makeNullable(), it.makeNullable()))
        }.toMap()

    private val booleanNot = builtIns.boolean.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("not"), NoLookupLocation.FROM_BACKEND).single()
    val booleanNotSymbol = symbolTable.referenceSimpleFunction(booleanNot)

    val eqeqeqSymbol = defineOperator(OperatorNames.EQEQEQ, booleanType, listOf(anyNType, anyNType))
    val eqeqSymbol = defineOperator(OperatorNames.EQEQ, booleanType, listOf(anyNType, anyNType))
    val throwCceSymbol = defineOperator(OperatorNames.THROW_CCE, nothingType, listOf())
    val throwIseSymbol = defineOperator(OperatorNames.THROW_ISE, nothingType, listOf())
    val andandSymbol = defineOperator(OperatorNames.ANDAND, booleanType, listOf(booleanType, booleanType))
    val ororSymbol = defineOperator(OperatorNames.OROR, booleanType, listOf(booleanType, booleanType))
    val noWhenBranchMatchedExceptionSymbol = defineOperator(OperatorNames.NO_WHEN_BRANCH_MATCHED_EXCEPTION, nothingType, listOf())
    val illegalArgumentExceptionSymbol = defineOperator(OperatorNames.ILLEGAL_ARGUMENT_EXCEPTION, nothingType, listOf(stringType))

    val checkNotNullSymbol = defineCheckNotNullOperator()

    private fun TypeConstructor.makeNonNullType() = KotlinTypeFactory.simpleType(Annotations.EMPTY, this, listOf(), false)
    private fun TypeConstructor.makeNullableType() = KotlinTypeFactory.simpleType(Annotations.EMPTY, this, listOf(), true)

    val dataClassArrayMemberHashCodeSymbol = defineOperator("dataClassArrayMemberHashCode", intType, listOf(anyType))

    val dataClassArrayMemberToStringSymbol = defineOperator("dataClassArrayMemberToString", stringType, listOf(anyNType))

    fun function(n: Int): IrClassSymbol = functionFactory.functionN(n).symbol
    fun suspendFunction(n: Int): IrClassSymbol = functionFactory.suspendFunctionN(n).symbol

    companion object {
        val KOTLIN_INTERNAL_IR_FQN = FqName("kotlin.internal.ir")
        val BUILTIN_OPERATOR = object : IrDeclarationOriginImpl("OPERATOR") {}
    }

    object OperatorNames {
        const val LESS = "less"
        const val LESS_OR_EQUAL = "lessOrEqual"
        const val GREATER = "greater"
        const val GREATER_OR_EQUAL = "greaterOrEqual"
        const val EQEQ = "EQEQ"
        const val EQEQEQ = "EQEQEQ"
        const val IEEE754_EQUALS = "ieee754equals"
        const val THROW_CCE = "THROW_CCE"
        const val THROW_ISE = "THROW_ISE"
        const val NO_WHEN_BRANCH_MATCHED_EXCEPTION = "noWhenBranchMatchedException"
        const val ILLEGAL_ARGUMENT_EXCEPTION = "illegalArgumentException"
        const val ANDAND = "ANDAND"
        const val OROR = "OROR"
    }
}
