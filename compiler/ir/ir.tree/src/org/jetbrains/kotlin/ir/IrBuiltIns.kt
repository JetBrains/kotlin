/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.addFakeOverrides
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.util.OperatorNameConventions

val IrClassSymbol.defaultTypeWithoutArguments: IrSimpleType
    get() = IrSimpleTypeImpl(
        classifier = this,
        nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL,
        arguments = emptyList(),
        annotations = emptyList()
    )

/**
 * Symbols for builtins that are available without any context and are not specific to any backend
 * (but specific to the frontend)
 */
@OptIn(InternalSymbolFinderAPI::class)
abstract class IrBuiltIns(override val symbolFinder: SymbolFinder) : SymbolFinderHolder {
    abstract val languageVersionSettings: LanguageVersionSettings

    abstract val irFactory: IrFactory

    val anyClass: IrClassSymbol = StandardClassIds.Any.classSymbol()
    val anyType: IrType = anyClass.defaultTypeWithoutArguments
    val anyNType: IrType = anyType.makeNullable()
    val booleanClass: IrClassSymbol = StandardClassIds.Boolean.classSymbol()
    val booleanType: IrType = booleanClass.defaultTypeWithoutArguments
    val charClass: IrClassSymbol = StandardClassIds.Char.classSymbol()
    val charType: IrType = charClass.defaultTypeWithoutArguments
    val numberClass: IrClassSymbol = StandardClassIds.Number.classSymbol()
    val numberType: IrType = numberClass.defaultTypeWithoutArguments
    val byteClass: IrClassSymbol = StandardClassIds.Byte.classSymbol()
    val byteType: IrType = byteClass.defaultTypeWithoutArguments
    val shortClass: IrClassSymbol = StandardClassIds.Short.classSymbol()
    val shortType: IrType = shortClass.defaultTypeWithoutArguments
    val intClass: IrClassSymbol = StandardClassIds.Int.classSymbol()
    val intType: IrType = intClass.defaultTypeWithoutArguments
    val longClass: IrClassSymbol = StandardClassIds.Long.classSymbol()
    val longType: IrType = longClass.defaultTypeWithoutArguments
    val ubyteClass: IrClassSymbol? = StandardClassIds.UByte.classSymbolOrNull()
    val ubyteType: IrType by lazy { ubyteClass!!.defaultTypeWithoutArguments }
    val ushortClass: IrClassSymbol? = StandardClassIds.UShort.classSymbolOrNull()
    val ushortType: IrType by lazy { ushortClass!!.defaultTypeWithoutArguments }
    val uintClass: IrClassSymbol? = StandardClassIds.UInt.classSymbolOrNull()
    val uintType: IrType by lazy { uintClass!!.defaultTypeWithoutArguments }
    val ulongClass: IrClassSymbol? = StandardClassIds.ULong.classSymbolOrNull()
    val ulongType: IrType by lazy { ulongClass!!.defaultTypeWithoutArguments }
    val floatClass: IrClassSymbol = StandardClassIds.Float.classSymbol()
    val floatType: IrType = floatClass.defaultTypeWithoutArguments
    val doubleClass: IrClassSymbol = StandardClassIds.Double.classSymbol()
    val doubleType: IrType = doubleClass.defaultTypeWithoutArguments
    val nothingClass: IrClassSymbol = StandardClassIds.Nothing.classSymbol()
    val nothingType: IrType = nothingClass.defaultTypeWithoutArguments
    val nothingNType: IrType = nothingClass.defaultTypeWithoutArguments.makeNullable()
    val unitClass: IrClassSymbol = StandardClassIds.Unit.classSymbol()
    val unitType: IrType = unitClass.defaultTypeWithoutArguments
    val stringClass: IrClassSymbol = StandardClassIds.String.classSymbol()
    val stringType: IrType = stringClass.defaultTypeWithoutArguments
    val charSequenceClass: IrClassSymbol = StandardClassIds.CharSequence.classSymbol()
    val enumClass: IrClassSymbol = StandardClassIds.Enum.classSymbol()

    val collectionClass: IrClassSymbol = StandardClassIds.Collection.classSymbol()
    val arrayClass: IrClassSymbol = StandardClassIds.Array.classSymbol()
    val setClass: IrClassSymbol = StandardClassIds.Set.classSymbol()
    val listClass: IrClassSymbol = StandardClassIds.List.classSymbol()
    val mapClass: IrClassSymbol = StandardClassIds.Map.classSymbol()
    val mapEntryClass: IrClassSymbol = StandardClassIds.MapEntry.classSymbol()
    val iterableClass: IrClassSymbol = StandardClassIds.Iterable.classSymbol()
    val iteratorClass: IrClassSymbol = StandardClassIds.Iterator.classSymbol()
    val listIteratorClass: IrClassSymbol = StandardClassIds.ListIterator.classSymbol()
    val mutableCollectionClass: IrClassSymbol = StandardClassIds.MutableCollection.classSymbol()
    val mutableSetClass: IrClassSymbol = StandardClassIds.MutableSet.classSymbol()
    val mutableListClass: IrClassSymbol = StandardClassIds.MutableList.classSymbol()
    val mutableMapClass: IrClassSymbol = StandardClassIds.MutableMap.classSymbol()
    val mutableMapEntryClass: IrClassSymbol = StandardClassIds.MutableMapEntry.classSymbol()
    val mutableIterableClass: IrClassSymbol = StandardClassIds.MutableIterable.classSymbol()
    val mutableIteratorClass: IrClassSymbol = StandardClassIds.MutableIterator.classSymbol()
    val mutableListIteratorClass: IrClassSymbol = StandardClassIds.MutableListIterator.classSymbol()

    val comparableClass: IrClassSymbol = StandardClassIds.Comparable.classSymbol()
    val throwableClass: IrClassSymbol = StandardClassIds.Throwable.classSymbol()
    val throwableType: IrType = throwableClass.defaultTypeWithoutArguments
    val kCallableClass: IrClassSymbol = StandardClassIds.KCallable.classSymbol()
    val kPropertyClass: IrClassSymbol = StandardClassIds.KProperty.classSymbol()
    val kClassClass: IrClassSymbol = StandardClassIds.KClass.classSymbol()
    val kTypeClass: IrClassSymbol = StandardClassIds.KType.classSymbol()
    val kProperty0Class: IrClassSymbol = StandardClassIds.KProperty0.classSymbol()
    val kProperty1Class: IrClassSymbol = StandardClassIds.KProperty1.classSymbol()
    val kProperty2Class: IrClassSymbol = StandardClassIds.KProperty2.classSymbol()
    val kMutableProperty0Class: IrClassSymbol = StandardClassIds.KMutableProperty0.classSymbol()
    val kMutableProperty1Class: IrClassSymbol = StandardClassIds.KMutableProperty1.classSymbol()
    val kMutableProperty2Class: IrClassSymbol = StandardClassIds.KMutableProperty2.classSymbol()
    val functionClass: IrClassSymbol = StandardClassIds.Function.classSymbol()
    val kFunctionClass: IrClassSymbol = StandardClassIds.KFunction.classSymbol()
    val annotationClass: IrClassSymbol = StandardClassIds.Annotation.classSymbol()
    val annotationType: IrType = annotationClass.defaultTypeWithoutArguments

    // TODO: consider removing to get rid of descriptor-related dependencies
    val primitiveTypeToIrType: Map<PrimitiveType, IrType> = mapOf(
        PrimitiveType.BOOLEAN to booleanType,
        PrimitiveType.CHAR to charType,
        PrimitiveType.BYTE to byteType,
        PrimitiveType.SHORT to shortType,
        PrimitiveType.INT to intType,
        PrimitiveType.LONG to longType,
        PrimitiveType.FLOAT to floatType,
        PrimitiveType.DOUBLE to doubleType
    )

    val primitiveIrTypes = listOf(booleanType, charType, byteType, shortType, intType, floatType, longType, doubleType)
    val primitiveIrTypesWithComparisons = listOf(charType, byteType, shortType, intType, floatType, longType, doubleType)
    val primitiveFloatingPointIrTypes = listOf(floatType, doubleType)

    private fun primitiveIterator(primitiveType: PrimitiveType): IrClassSymbol {
        val classId = ClassId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier("${primitiveType.typeName}Iterator"))
        return classId.classSymbol()
    }

    val booleanIterator: IrClassSymbol = primitiveIterator(PrimitiveType.BOOLEAN)
    val charIterator: IrClassSymbol = primitiveIterator(PrimitiveType.CHAR)
    val byteIterator: IrClassSymbol = primitiveIterator(PrimitiveType.BYTE)
    val shortIterator: IrClassSymbol = primitiveIterator(PrimitiveType.SHORT)
    val intIterator: IrClassSymbol = primitiveIterator(PrimitiveType.INT)
    val longIterator: IrClassSymbol = primitiveIterator(PrimitiveType.LONG)
    val floatIterator: IrClassSymbol = primitiveIterator(PrimitiveType.FLOAT)
    val doubleIterator: IrClassSymbol = primitiveIterator(PrimitiveType.DOUBLE)

    private fun primitiveArray(primitiveType: PrimitiveType): IrClassSymbol {
        val classId = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("${primitiveType.typeName}Array"))
        return classId.classSymbol()
    }

    val booleanArray: IrClassSymbol = primitiveArray(PrimitiveType.BOOLEAN)
    val charArray: IrClassSymbol = primitiveArray(PrimitiveType.CHAR)
    val byteArray: IrClassSymbol = primitiveArray(PrimitiveType.BYTE)
    val shortArray: IrClassSymbol = primitiveArray(PrimitiveType.SHORT)
    val intArray: IrClassSymbol = primitiveArray(PrimitiveType.INT)
    val longArray: IrClassSymbol = primitiveArray(PrimitiveType.LONG)
    val floatArray: IrClassSymbol = primitiveArray(PrimitiveType.FLOAT)
    val doubleArray: IrClassSymbol = primitiveArray(PrimitiveType.DOUBLE)

    private fun unsignedPrimitiveArray(unsignedType: UnsignedType): IrClassSymbol? {
        val classId = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("${unsignedType.typeName}Array"))
        return classId.classSymbolOrNull()
    }

    val ubyteArray: IrClassSymbol? = unsignedPrimitiveArray(UnsignedType.UBYTE)
    val ushortArray: IrClassSymbol? = unsignedPrimitiveArray(UnsignedType.USHORT)
    val uintArray: IrClassSymbol? = unsignedPrimitiveArray(UnsignedType.UINT)
    val ulongArray: IrClassSymbol? = unsignedPrimitiveArray(UnsignedType.ULONG)

    val primitiveArraysToPrimitiveTypes: Map<IrClassSymbol, PrimitiveType> = PrimitiveType.entries.associateBy { primitiveArray(it) }
    val primitiveTypesToPrimitiveArrays: Map<PrimitiveType, IrClassSymbol> = PrimitiveType.entries.associateWith { primitiveArray(it) }
    val primitiveArrayElementTypes: Map<IrClassSymbol, IrType?> = primitiveArraysToPrimitiveTypes.mapValues { primitiveTypeToIrType[it.value] }
    val primitiveArrayForType: Map<IrType?, IrClassSymbol> = primitiveArrayElementTypes.entries.associate { it.value to it.key }

    val unsignedTypesToUnsignedArrays: Map<UnsignedType, IrClassSymbol> =
        UnsignedType.entries.mapNotNull { unsignedType ->
            val array = unsignedType.arrayClassId.classSymbolOrNull()
            if (array == null) null else unsignedType to array
        }.toMap()

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    val unsignedArraysElementTypes: Map<IrClassSymbol, IrType?> =
        unsignedTypesToUnsignedArrays.map { (k, v) ->
            v to k.classId.classSymbolOrNull()?.defaultTypeWithoutArguments
        }.toMap()

    val arrays: List<IrClassSymbol> = primitiveTypesToPrimitiveArrays.values + unsignedTypesToUnsignedArrays.values + arrayClass

    val deprecatedSymbol: IrClassSymbol = StandardClassIds.Annotations.Deprecated.classSymbol()
    val deprecationLevelSymbol: IrClassSymbol = StandardClassIds.DeprecationLevel.classSymbol()

    abstract val lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    abstract val lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    abstract val greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    abstract val greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    abstract val ieee754equalsFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    abstract val booleanNotSymbol: IrSimpleFunctionSymbol
    abstract val eqeqeqSymbol: IrSimpleFunctionSymbol
    abstract val eqeqSymbol: IrSimpleFunctionSymbol
    abstract val throwCceSymbol: IrSimpleFunctionSymbol
    abstract val throwIseSymbol: IrSimpleFunctionSymbol
    abstract val andandSymbol: IrSimpleFunctionSymbol
    abstract val ororSymbol: IrSimpleFunctionSymbol
    abstract val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol
    abstract val illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol
    abstract val checkNotNullSymbol: IrSimpleFunctionSymbol
    abstract val dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol
    abstract val dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol
    abstract val linkageErrorSymbol: IrSimpleFunctionSymbol

    val intPlusSymbol: IrSimpleFunctionSymbol by CallableId(StandardClassIds.Int, OperatorNameConventions.PLUS)
        .functionSymbol { it.parameters[1].type == intType }

    val intTimesSymbol: IrSimpleFunctionSymbol by CallableId(StandardClassIds.Int, OperatorNameConventions.TIMES)
        .functionSymbol { it.parameters[1].type == intType }

    val intXorSymbol: IrSimpleFunctionSymbol by CallableId(StandardClassIds.Int, OperatorNameConventions.XOR)
        .functionSymbol { it.parameters[1].type == intType }

    val intAndSymbol: IrSimpleFunctionSymbol by CallableId(StandardClassIds.Int, OperatorNameConventions.AND)
        .functionSymbol { it.parameters[1].type == intType }

    val arrayOf: IrSimpleFunctionSymbol = CallableId(StandardClassIds.BASE_KOTLIN_PACKAGE, ArrayFqNames.ARRAY_OF_FUNCTION).functionSymbol()
    val arrayOfNulls: IrSimpleFunctionSymbol = CallableId(StandardClassIds.BASE_KOTLIN_PACKAGE, ArrayFqNames.ARRAY_OF_NULLS_FUNCTION).functionSymbol()

    abstract fun functionN(arity: Int): IrClass
    abstract fun kFunctionN(arity: Int): IrClass
    abstract fun suspendFunctionN(arity: Int): IrClass
    abstract fun kSuspendFunctionN(arity: Int): IrClass

    fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol = when (n) {
        0 -> if (mutable) kMutableProperty0Class else kProperty0Class
        1 -> if (mutable) kMutableProperty1Class else kProperty1Class
        2 -> if (mutable) kMutableProperty2Class else kProperty2Class
        else -> error("No KProperty for n=$n mutable=$mutable")
    }

    abstract val operatorsPackageFragment: IrExternalPackageFragment
    abstract val kotlinInternalPackageFragment: IrExternalPackageFragment

    protected fun createIntrinsicConstEvaluationClass(): IrClass {
        return irFactory.buildClass {
            name = StandardClassIds.Annotations.IntrinsicConstEvaluation.shortClassName
            kind = ClassKind.ANNOTATION_CLASS
            modality = Modality.FINAL
        }.apply {
            parent = kotlinInternalPackageFragment
            createThisReceiverParameter()
            addConstructor { isPrimary = true }
            addFakeOverrides(IrTypeSystemContextImpl(this@IrBuiltIns))
        }
    }

    companion object {
        val KOTLIN_INTERNAL_IR_FQN = FqName("kotlin.internal.ir")
        val BUILTIN_OPERATOR = IrDeclarationOriginImpl("OPERATOR")
    }
}

object BuiltInOperatorNames {
    const val LESS = "less"
    const val LESS_OR_EQUAL = "lessOrEqual"
    const val GREATER = "greater"
    const val GREATER_OR_EQUAL = "greaterOrEqual"
    const val COMPARE_TO = "compareTo"
    const val EQEQ = "EQEQ"
    const val EQEQEQ = "EQEQEQ"
    const val IEEE754_EQUALS = "ieee754equals"
    const val THROW_CCE = "THROW_CCE"
    const val THROW_ISE = "THROW_ISE"
    const val NO_WHEN_BRANCH_MATCHED_EXCEPTION = "noWhenBranchMatchedException"
    const val ILLEGAL_ARGUMENT_EXCEPTION = "illegalArgumentException"
    const val ANDAND = "ANDAND"
    const val OROR = "OROR"
    const val CHECK_NOT_NULL = "CHECK_NOT_NULL"
}
