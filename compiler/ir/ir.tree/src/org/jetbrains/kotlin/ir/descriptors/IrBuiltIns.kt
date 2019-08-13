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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.withHasQuestionMark
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.makeNullable

class IrBuiltIns(
    val builtIns: KotlinBuiltIns,
    private val typeTranslator: TypeTranslator,
    outerSymbolTable: SymbolTable? = null
) {
    val languageVersionSettings = typeTranslator.languageVersionSettings

    private val builtInsModule = builtIns.builtInsModule

    private val packageFragment = IrBuiltinsPackageFragmentDescriptorImpl(builtInsModule, KOTLIN_INTERNAL_IR_FQN)
    val irBuiltInsSymbols = mutableListOf<IrSimpleFunctionSymbol>()

    private val symbolTable = outerSymbolTable ?: SymbolTable()

    private fun ClassDescriptor.toIrSymbol() = symbolTable.referenceClass(this)
    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun defineOperator(name: String, returnType: KotlinType, valueParameterTypes: List<KotlinType>): IrSimpleFunctionSymbol {
        val operatorDescriptor = IrSimpleBuiltinOperatorDescriptorImpl(packageFragment, Name.identifier(name), returnType)
        for ((i, valueParameterType) in valueParameterTypes.withIndex()) {
            operatorDescriptor.addValueParameter(
                IrBuiltinValueParameterDescriptorImpl(operatorDescriptor, Name.identifier("arg$i"), i, valueParameterType)
            )
        }
        return operatorDescriptor.addStub()
    }

    private fun <T : SimpleFunctionDescriptor> T.addStub(): IrSimpleFunctionSymbol =
        symbolTable.referenceSimpleFunction(this).also {
            irBuiltInsSymbols += it
        }

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

    val collectionClass = builtIns.collection.toIrSymbol()

    val arrayClass = builtIns.array.toIrSymbol()

    val throwableType = builtIns.throwable.defaultType.toIrType()
    val throwableClass = builtIns.throwable.toIrSymbol()

    private class BuiltInIrTypePair(val type: IrType) {
        val nType: IrType = with(type as IrSimpleType) {
            IrSimpleTypeImpl(classifier, true, arguments, annotations)
        }
    }

    private val primitiveTypesMapping = mapOf(
        builtIns.any to BuiltInIrTypePair(anyType),
        builtIns.boolean to BuiltInIrTypePair(booleanType),
        builtIns.char to BuiltInIrTypePair(charType),
        builtIns.number to BuiltInIrTypePair(numberType),
        builtIns.byte to BuiltInIrTypePair(byteType),
        builtIns.short to BuiltInIrTypePair(shortType),
        builtIns.int to BuiltInIrTypePair(intType),
        builtIns.long to BuiltInIrTypePair(longType),
        builtIns.float to BuiltInIrTypePair(floatType),
        builtIns.double to BuiltInIrTypePair(doubleType),
        builtIns.nothing to BuiltInIrTypePair(nothingType),
        builtIns.unit to BuiltInIrTypePair(unitType),
        builtIns.string to BuiltInIrTypePair(stringType),
        builtIns.throwable to BuiltInIrTypePair(throwableType)
    )

    fun getPrimitiveTypeOrNullByDescriptor(descriptor: ClassifierDescriptor, isNullable: Boolean) =
        primitiveTypesMapping[descriptor]?.let {
            if (isNullable) it.nType else it.type
        } as IrSimpleType?

    val primitiveIrTypes by lazy { listOf(booleanType, charType, byteType, shortType, intType, floatType, longType, doubleType) }

    val kCallableClass = builtIns.kCallable.toIrSymbol()
    val kPropertyClass = builtIns.kProperty.toIrSymbol()
    val kDeclarationContainerClass = builtIns.kDeclarationContainer.toIrSymbol()
    val kClassClass = builtIns.kClass.toIrSymbol()

    private val kProperty0Class = builtIns.kProperty0.toIrSymbol()
    private val kProperty1Class = builtIns.kProperty1.toIrSymbol()
    private val kProperty2Class = builtIns.kProperty2.toIrSymbol()
    private val kMutableProperty0Class = builtIns.kMutableProperty0.toIrSymbol()
    private val kMutableProperty1Class = builtIns.kMutableProperty1.toIrSymbol()
    private val kMutableProperty2Class = builtIns.kMutableProperty2.toIrSymbol()

    fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol = when (n) {
        0 -> if (mutable) kMutableProperty0Class else kProperty0Class
        1 -> if (mutable) kMutableProperty1Class else kProperty1Class
        2 -> if (mutable) kMutableProperty2Class else kProperty2Class
        else -> error("No KProperty for n=$n mutable=$mutable")
    }

    // TODO switch to IrType
    val primitiveTypes = listOf(bool, char, byte, short, int, long, float, double)
    val primitiveTypesWithComparisons = listOf(char, byte, short, int, long, float, double)
    val primitiveFloatingPointTypes = listOf(float, double)
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

    val lessFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType(OperatorNames.LESS)
    val lessOrEqualFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType(OperatorNames.LESS_OR_EQUAL)
    val greaterOrEqualFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType(OperatorNames.GREATER_OR_EQUAL)
    val greaterFunByOperandType = primitiveTypesWithComparisons.defineComparisonOperatorForEachType(OperatorNames.GREATER)

    val ieee754equalsFunByOperandType =
        primitiveFloatingPointTypes.associateWith {
            defineOperator(OperatorNames.IEEE754_EQUALS, bool, listOf(it.makeNullable(), it.makeNullable()))
        }

    val booleanNot = builtIns.boolean.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("not"), NoLookupLocation.FROM_BACKEND).single()
    val booleanNotSymbol = symbolTable.referenceSimpleFunction(booleanNot)

    val eqeqeqSymbol = defineOperator(OperatorNames.EQEQEQ, bool, listOf(anyN, anyN))
    val eqeqSymbol = defineOperator(OperatorNames.EQEQ, bool, listOf(anyN, anyN))
    val throwCceSymbol = defineOperator(OperatorNames.THROW_CCE, nothing, listOf())
    val throwIseSymbol = defineOperator(OperatorNames.THROW_ISE, nothing, listOf())
    val andandSymbol = defineOperator(OperatorNames.ANDAND, bool, listOf(bool, bool))
    val ororSymbol = defineOperator(OperatorNames.OROR, bool, listOf(bool, bool))
    val noWhenBranchMatchedExceptionSymbol = defineOperator(OperatorNames.NO_WHEN_BRANCH_MATCHED_EXCEPTION, nothing, listOf())
    val illegalArgumentExceptionSymbol = defineOperator(OperatorNames.ILLEGAL_ARGUMENT_EXCEPTION, nothing, listOf(string))

    val eqeqeq = eqeqeqSymbol.descriptor
    val eqeq = eqeqSymbol.descriptor
    val throwCce = throwCceSymbol.descriptor
    val noWhenBranchMatchedException = noWhenBranchMatchedExceptionSymbol.descriptor
    val illegalArgumentException = illegalArgumentExceptionSymbol.descriptor

    val enumValueOfSymbol =
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

            val returnType = typeParameterT.typeConstructor.makeNonNullType()

            initialize(null, null, listOf(typeParameterT), listOf(valueParameterName), returnType, Modality.FINAL, Visibilities.PUBLIC)
        }.addStub()
    val enumValueOf = enumValueOfSymbol.descriptor

    val checkNotNullSymbol =
        SimpleFunctionDescriptorImpl.create(
            packageFragment,
            Annotations.EMPTY,
            Name.identifier("CHECK_NOT_NULL"),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        ).apply {
            val typeParameterT = TypeParameterDescriptorImpl.createForFurtherModification(
                this, Annotations.EMPTY, false, Variance.INVARIANT, Name.identifier("T"), 0, SourceElement.NO_SOURCE
            ).apply {
                addUpperBound(builtIns.anyType)
                setInitialized()
            }

            val valueParameterX = ValueParameterDescriptorImpl(
                this, null, 0, Annotations.EMPTY, Name.identifier("x"), typeParameterT.typeConstructor.makeNullableType(),
                false, false, false, null, SourceElement.NO_SOURCE
            )

            initialize(
                null, null,
                listOf(typeParameterT), listOf(valueParameterX), typeParameterT.typeConstructor.makeNonNullType(),
                Modality.FINAL, Visibilities.PUBLIC
            )
        }.addStub()
    val checkNotNull = checkNotNullSymbol.descriptor

    private fun TypeConstructor.makeNonNullType() = KotlinTypeFactory.simpleType(Annotations.EMPTY, this, listOf(), false)
    private fun TypeConstructor.makeNullableType() = KotlinTypeFactory.simpleType(Annotations.EMPTY, this, listOf(), true)

    val dataClassArrayMemberHashCodeSymbol = defineOperator("dataClassArrayMemberHashCode", int, listOf(any))
    val dataClassArrayMemberHashCode = dataClassArrayMemberHashCodeSymbol.descriptor

    val dataClassArrayMemberToStringSymbol = defineOperator("dataClassArrayMemberToString", string, listOf(anyN))
    val dataClassArrayMemberToString = dataClassArrayMemberToStringSymbol.descriptor

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
        const val THROW_CCE = "THROW_CCE"
        const val THROW_ISE = "THROW_ISE"
        const val NO_WHEN_BRANCH_MATCHED_EXCEPTION = "noWhenBranchMatchedException"
        const val ILLEGAL_ARGUMENT_EXCEPTION = "illegalArgumentException"
        const val ANDAND = "ANDAND"
        const val OROR = "OROR"
    }
}
