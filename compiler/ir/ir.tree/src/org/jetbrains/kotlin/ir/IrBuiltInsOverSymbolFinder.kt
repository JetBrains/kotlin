/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.util.OperatorNameConventions

private val IrClassSymbol.defaultTypeWithoutArguments: IrSimpleType
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
abstract class IrBuiltInsOverSymbolFinder(override val symbolFinder: SymbolFinder) : IrBuiltIns() {
    override val anyClass: IrClassSymbol = StandardClassIds.Any.classSymbol()
    override val anyType: IrType = anyClass.defaultTypeWithoutArguments
    override val anyNType: IrType = anyType.makeNullable()
    override val booleanClass: IrClassSymbol = StandardClassIds.Boolean.classSymbol()
    override val booleanType: IrType = booleanClass.defaultTypeWithoutArguments
    override val charClass: IrClassSymbol = StandardClassIds.Char.classSymbol()
    override val charType: IrType = charClass.defaultTypeWithoutArguments
    override val numberClass: IrClassSymbol = StandardClassIds.Number.classSymbol()
    override val numberType: IrType = numberClass.defaultTypeWithoutArguments
    override val byteClass: IrClassSymbol = StandardClassIds.Byte.classSymbol()
    override val byteType: IrType = byteClass.defaultTypeWithoutArguments
    override val shortClass: IrClassSymbol = StandardClassIds.Short.classSymbol()
    override val shortType: IrType = shortClass.defaultTypeWithoutArguments
    override val intClass: IrClassSymbol = StandardClassIds.Int.classSymbol()
    override val intType: IrType = intClass.defaultTypeWithoutArguments
    override val longClass: IrClassSymbol = StandardClassIds.Long.classSymbol()
    override val longType: IrType = longClass.defaultTypeWithoutArguments
    override val ubyteClass: IrClassSymbol? = StandardClassIds.UByte.classSymbolOrNull()
    override val ubyteType: IrType by lazy { ubyteClass!!.defaultTypeWithoutArguments }
    override val ushortClass: IrClassSymbol? = StandardClassIds.UShort.classSymbolOrNull()
    override val ushortType: IrType by lazy { ushortClass!!.defaultTypeWithoutArguments }
    override val uintClass: IrClassSymbol? = StandardClassIds.UInt.classSymbolOrNull()
    override val uintType: IrType by lazy { uintClass!!.defaultTypeWithoutArguments }
    override val ulongClass: IrClassSymbol? = StandardClassIds.ULong.classSymbolOrNull()
    override val ulongType: IrType by lazy { ulongClass!!.defaultTypeWithoutArguments }
    override val floatClass: IrClassSymbol = StandardClassIds.Float.classSymbol()
    override val floatType: IrType = floatClass.defaultTypeWithoutArguments
    override val doubleClass: IrClassSymbol = StandardClassIds.Double.classSymbol()
    override val doubleType: IrType = doubleClass.defaultTypeWithoutArguments
    override val nothingClass: IrClassSymbol = StandardClassIds.Nothing.classSymbol()
    override val nothingType: IrType = nothingClass.defaultTypeWithoutArguments
    override val nothingNType: IrType = nothingClass.defaultTypeWithoutArguments.makeNullable()
    override val unitClass: IrClassSymbol = StandardClassIds.Unit.classSymbol()
    override val unitType: IrType = unitClass.defaultTypeWithoutArguments
    override val stringClass: IrClassSymbol = StandardClassIds.String.classSymbol()
    override val stringType: IrType = stringClass.defaultTypeWithoutArguments
    override val charSequenceClass: IrClassSymbol = StandardClassIds.CharSequence.classSymbol()

    override val collectionClass: IrClassSymbol = StandardClassIds.Collection.classSymbol()
    override val arrayClass: IrClassSymbol = StandardClassIds.Array.classSymbol()
    override val setClass: IrClassSymbol = StandardClassIds.Set.classSymbol()
    override val listClass: IrClassSymbol = StandardClassIds.List.classSymbol()
    override val mapClass: IrClassSymbol = StandardClassIds.Map.classSymbol()
    override val mapEntryClass: IrClassSymbol = StandardClassIds.MapEntry.classSymbol()
    override val iterableClass: IrClassSymbol = StandardClassIds.Iterable.classSymbol()
    override val iteratorClass: IrClassSymbol = StandardClassIds.Iterator.classSymbol()
    override val listIteratorClass: IrClassSymbol = StandardClassIds.ListIterator.classSymbol()
    override val mutableCollectionClass: IrClassSymbol = StandardClassIds.MutableCollection.classSymbol()
    override val mutableSetClass: IrClassSymbol = StandardClassIds.MutableSet.classSymbol()
    override val mutableListClass: IrClassSymbol = StandardClassIds.MutableList.classSymbol()
    override val mutableMapClass: IrClassSymbol = StandardClassIds.MutableMap.classSymbol()
    override val mutableMapEntryClass: IrClassSymbol = StandardClassIds.MutableMapEntry.classSymbol()
    override val mutableIterableClass: IrClassSymbol = StandardClassIds.MutableIterable.classSymbol()
    override val mutableIteratorClass: IrClassSymbol = StandardClassIds.MutableIterator.classSymbol()
    override val mutableListIteratorClass: IrClassSymbol = StandardClassIds.MutableListIterator.classSymbol()

    override val comparableClass: IrClassSymbol = StandardClassIds.Comparable.classSymbol()
    override val throwableClass: IrClassSymbol = StandardClassIds.Throwable.classSymbol()
    override val throwableType: IrType = throwableClass.defaultTypeWithoutArguments
    override val kCallableClass: IrClassSymbol = StandardClassIds.KCallable.classSymbol()
    override val kPropertyClass: IrClassSymbol = StandardClassIds.KProperty.classSymbol()
    override val kClassClass: IrClassSymbol = StandardClassIds.KClass.classSymbol()
    override val kTypeClass: IrClassSymbol = StandardClassIds.KType.classSymbol()
    override val kProperty0Class: IrClassSymbol = StandardClassIds.KProperty0.classSymbol()
    override val kProperty1Class: IrClassSymbol = StandardClassIds.KProperty1.classSymbol()
    override val kProperty2Class: IrClassSymbol = StandardClassIds.KProperty2.classSymbol()
    override val kMutableProperty0Class: IrClassSymbol = StandardClassIds.KMutableProperty0.classSymbol()
    override val kMutableProperty1Class: IrClassSymbol = StandardClassIds.KMutableProperty1.classSymbol()
    override val kMutableProperty2Class: IrClassSymbol = StandardClassIds.KMutableProperty2.classSymbol()
    override val functionClass: IrClassSymbol = StandardClassIds.Function.classSymbol()
    override val kFunctionClass: IrClassSymbol = StandardClassIds.KFunction.classSymbol()
    override val annotationClass: IrClassSymbol = StandardClassIds.Annotation.classSymbol()
    override val annotationType: IrType = annotationClass.defaultTypeWithoutArguments

    override val primitiveTypeToIrType: Map<PrimitiveType, IrType> = mapOf(
        PrimitiveType.BOOLEAN to booleanType,
        PrimitiveType.CHAR to charType,
        PrimitiveType.BYTE to byteType,
        PrimitiveType.SHORT to shortType,
        PrimitiveType.INT to intType,
        PrimitiveType.LONG to longType,
        PrimitiveType.FLOAT to floatType,
        PrimitiveType.DOUBLE to doubleType
    )

    override val primitiveIrTypes = listOf(booleanType, charType, byteType, shortType, intType, floatType, longType, doubleType)
    override val primitiveIrTypesWithComparisons = listOf(charType, byteType, shortType, intType, floatType, longType, doubleType)
    override val primitiveFloatingPointIrTypes = listOf(floatType, doubleType)

    private fun primitiveIterator(primitiveType: PrimitiveType): IrClassSymbol {
        val classId = ClassId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier("${primitiveType.typeName}Iterator"))
        return classId.classSymbol()
    }

    override val byteIterator: IrClassSymbol = primitiveIterator(PrimitiveType.BYTE)
    override val charIterator: IrClassSymbol = primitiveIterator(PrimitiveType.CHAR)
    override val shortIterator: IrClassSymbol = primitiveIterator(PrimitiveType.SHORT)
    override val intIterator: IrClassSymbol = primitiveIterator(PrimitiveType.INT)
    override val longIterator: IrClassSymbol = primitiveIterator(PrimitiveType.LONG)
    override val floatIterator: IrClassSymbol = primitiveIterator(PrimitiveType.FLOAT)
    override val doubleIterator: IrClassSymbol = primitiveIterator(PrimitiveType.DOUBLE)
    override val booleanIterator: IrClassSymbol = primitiveIterator(PrimitiveType.BOOLEAN)

    private fun primitiveArray(primitiveType: PrimitiveType): IrClassSymbol {
        val classId = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("${primitiveType.typeName}Array"))
        return classId.classSymbol()
    }

    override val primitiveArraysToPrimitiveTypes: Map<IrClassSymbol, PrimitiveType> = PrimitiveType.entries.associateBy { primitiveArray(it) }
    override val primitiveTypesToPrimitiveArrays: Map<PrimitiveType, IrClassSymbol> = primitiveArraysToPrimitiveTypes.entries.associate { it.value to it.key }
    override val primitiveArrayElementTypes: Map<IrClassSymbol, IrType?> = primitiveArraysToPrimitiveTypes.mapValues { primitiveTypeToIrType[it.value] }
    override val primitiveArrayForType: Map<IrType?, IrClassSymbol> = primitiveArrayElementTypes.entries.associate { it.value to it.key }

    override val byteArray: IrClassSymbol = primitiveTypesToPrimitiveArrays[PrimitiveType.BYTE]!!
    override val charArray: IrClassSymbol = primitiveTypesToPrimitiveArrays[PrimitiveType.CHAR]!!
    override val shortArray: IrClassSymbol = primitiveTypesToPrimitiveArrays[PrimitiveType.SHORT]!!
    override val intArray: IrClassSymbol = primitiveTypesToPrimitiveArrays[PrimitiveType.INT]!!
    override val longArray: IrClassSymbol = primitiveTypesToPrimitiveArrays[PrimitiveType.LONG]!!
    override val floatArray: IrClassSymbol = primitiveTypesToPrimitiveArrays[PrimitiveType.FLOAT]!!
    override val doubleArray: IrClassSymbol = primitiveTypesToPrimitiveArrays[PrimitiveType.DOUBLE]!!
    override val booleanArray: IrClassSymbol = primitiveTypesToPrimitiveArrays[PrimitiveType.BOOLEAN]!!

    override val unsignedTypesToUnsignedArrays: Map<UnsignedType, IrClassSymbol> =
        UnsignedType.entries.mapNotNull { unsignedType ->
            val array = unsignedType.arrayClassId.classSymbolOrNull()
            if (array == null) null else unsignedType to array
        }.toMap()

    override val ubyteArray: IrClassSymbol? = unsignedTypesToUnsignedArrays[UnsignedType.UBYTE]
    override val ushortArray: IrClassSymbol? = unsignedTypesToUnsignedArrays[UnsignedType.USHORT]
    override val uintArray: IrClassSymbol? = unsignedTypesToUnsignedArrays[UnsignedType.UINT]
    override val ulongArray: IrClassSymbol? = unsignedTypesToUnsignedArrays[UnsignedType.ULONG]

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override val unsignedArraysElementTypes: Map<IrClassSymbol, IrType?> = buildMap {
        ubyteArray?.let { array -> put(array, ubyteType) }
        ushortArray?.let { array -> put(array, ushortType) }
        uintArray?.let { array -> put(array, uintType) }
        ulongArray?.let { array -> put(array, ulongType) }
    }

    override val enumClass: IrClassSymbol = StandardClassIds.Enum.classSymbol()

    override val intPlusSymbol: IrSimpleFunctionSymbol by CallableId(StandardClassIds.Int, OperatorNameConventions.PLUS)
        .functionSymbol { it.parameters[1].type == intType }
    override val intTimesSymbol: IrSimpleFunctionSymbol by CallableId(StandardClassIds.Int, OperatorNameConventions.TIMES)
        .functionSymbol { it.parameters[1].type == intType }
    override val intXorSymbol: IrSimpleFunctionSymbol by CallableId(StandardClassIds.Int, OperatorNameConventions.XOR)
        .functionSymbol { it.parameters[1].type == intType }
    override val intAndSymbol: IrSimpleFunctionSymbol by CallableId(StandardClassIds.Int, OperatorNameConventions.AND)
        .functionSymbol { it.parameters[1].type == intType }

    override val arrayOf: IrSimpleFunctionSymbol = CallableId(StandardClassIds.BASE_KOTLIN_PACKAGE, ArrayFqNames.ARRAY_OF_FUNCTION).functionSymbol()
    override val arrayOfNulls: IrSimpleFunctionSymbol = CallableId(StandardClassIds.BASE_KOTLIN_PACKAGE, ArrayFqNames.ARRAY_OF_NULLS_FUNCTION).functionSymbol()

    override val deprecatedSymbol: IrClassSymbol = StandardClassIds.Annotations.Deprecated.classSymbol()
    override val deprecationLevelSymbol: IrClassSymbol = StandardClassIds.DeprecationLevel.classSymbol()

    override fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol = when (n) {
        0 -> if (mutable) kMutableProperty0Class else kProperty0Class
        1 -> if (mutable) kMutableProperty1Class else kProperty1Class
        2 -> if (mutable) kMutableProperty2Class else kProperty2Class
        else -> error("No KProperty for n=$n mutable=$mutable")
    }
}
