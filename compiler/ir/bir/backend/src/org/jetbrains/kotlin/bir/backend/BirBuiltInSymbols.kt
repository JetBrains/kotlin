/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI

@OptIn(ObsoleteDescriptorBasedAPI::class)
open class BirBuiltInSymbols(
    irSymbols: Symbols,
    converter: Ir2BirConverter,
) {
    val throwNullPointerException: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.throwNullPointerException)
    val throwTypeCastException: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.throwTypeCastException)
    val throwUninitializedPropertyAccessException: BirSimpleFunctionSymbol =
        converter.remapSymbol(irSymbols.throwUninitializedPropertyAccessException)
    val throwKotlinNothingValueException: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.throwKotlinNothingValueException)
    val stringBuilder: BirClassSymbol = converter.remapSymbol(irSymbols.stringBuilder)
    val defaultConstructorMarker: BirClassSymbol = converter.remapSymbol(irSymbols.defaultConstructorMarker)
    val continuationClass: BirClassSymbol = converter.remapSymbol(irSymbols.continuationClass)
    val functionAdapter: BirClassSymbol = converter.remapSymbol(irSymbols.functionAdapter)
    val unsafeCoerceIntrinsic: BirSimpleFunctionSymbol? = irSymbols.unsafeCoerceIntrinsic?.let { converter.remapSymbol(it) }
    val arraysContentEquals = irSymbols.arraysContentEquals?.entries?.associate {
        converter.remapType(it.key) to converter.remapSymbol<_, BirSimpleFunctionSymbol>(it.value)
    }
    val iterator: BirClassSymbol = converter.remapSymbol(irSymbols.iterator)
    val charSequence: BirClassSymbol = converter.remapSymbol(irSymbols.charSequence)
    val string: BirClassSymbol = converter.remapSymbol(irSymbols.string)
    val primitiveIteratorsByType = irSymbols.primitiveIteratorsByType.mapValues { converter.remapSymbol<_, BirClassSymbol>(it.value) }
    val asserts: List<BirSimpleFunctionSymbol> = irSymbols.asserts.map { converter.remapSymbol(it) }
    val uByte: BirClassSymbol? = irSymbols.uByte?.let { converter.remapSymbol(it) }
    val uShort: BirClassSymbol? = irSymbols.uShort?.let { converter.remapSymbol(it) }
    val uInt: BirClassSymbol? = irSymbols.uInt?.let { converter.remapSymbol(it) }
    val uLong: BirClassSymbol? = irSymbols.uLong?.let { converter.remapSymbol(it) }
    val uIntProgression: BirClassSymbol? = irSymbols.uIntProgression?.let { converter.remapSymbol(it) }
    val uLongProgression: BirClassSymbol? = irSymbols.uLongProgression?.let { converter.remapSymbol(it) }
    val uIntRange: BirClassSymbol? = irSymbols.uIntRange?.let { converter.remapSymbol(it) }
    val uLongRange: BirClassSymbol? = irSymbols.uLongRange?.let { converter.remapSymbol(it) }
    val sequence: BirClassSymbol? = irSymbols.sequence?.let { converter.remapSymbol(it) }
    val charProgression: BirClassSymbol = converter.remapSymbol(irSymbols.charProgression)
    val intProgression: BirClassSymbol = converter.remapSymbol(irSymbols.intProgression)
    val longProgression: BirClassSymbol = converter.remapSymbol(irSymbols.longProgression)
    val progressionClasses: List<BirClassSymbol> = irSymbols.progressionClasses.map { converter.remapSymbol(it) }
    val charRange: BirClassSymbol = converter.remapSymbol(irSymbols.charRange)
    val intRange: BirClassSymbol = converter.remapSymbol(irSymbols.intRange)
    val longRange: BirClassSymbol = converter.remapSymbol(irSymbols.longRange)
    val rangeClasses: List<BirClassSymbol> = irSymbols.rangeClasses.map { converter.remapSymbol(it) }
    val closedRange: BirClassSymbol = converter.remapSymbol(irSymbols.closedRange)
    val getProgressionLastElementByReturnType =
        irSymbols.getProgressionLastElementByReturnType.entries.associate {
            converter.remapSymbol<_, BirClassifierSymbol>(it.key) to
                    converter.remapSymbol<_, BirSimpleFunctionSymbol>(it.value)
        }
    val toUIntByExtensionReceiver =
        irSymbols.toUIntByExtensionReceiver.entries.associate {
            converter.remapSymbol<_, BirClassifierSymbol>(it.key) to
                    converter.remapSymbol<_, BirSimpleFunctionSymbol>(it.value)
        }
    val toULongByExtensionReceiver =
        irSymbols.toULongByExtensionReceiver.entries.associate {
            converter.remapSymbol<_, BirClassifierSymbol>(it.key) to
                    converter.remapSymbol<_, BirSimpleFunctionSymbol>(it.value)
        }
    val any: BirClassSymbol = converter.remapSymbol(irSymbols.any)
    val unit: BirClassSymbol = converter.remapSymbol(irSymbols.unit)
    val char: BirClassSymbol = converter.remapSymbol(irSymbols.char)
    val byte: BirClassSymbol = converter.remapSymbol(irSymbols.byte)
    val short: BirClassSymbol = converter.remapSymbol(irSymbols.short)
    val int: BirClassSymbol = converter.remapSymbol(irSymbols.int)
    val long: BirClassSymbol = converter.remapSymbol(irSymbols.long)
    val float: BirClassSymbol = converter.remapSymbol(irSymbols.float)
    val double: BirClassSymbol = converter.remapSymbol(irSymbols.double)
    val integerClasses: List<BirClassSymbol> = irSymbols.integerClasses.map { converter.remapSymbol(it) }
    val progressionElementTypes: List<BirType> = irSymbols.progressionElementTypes.map { converter.remapType(it) }
    val arrayOf: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.arrayOf)
    val arrayOfNulls: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.arrayOfNulls)
    val array: BirClassSymbol = converter.remapSymbol(irSymbols.array)
    val byteArray: BirClassSymbol = converter.remapSymbol(irSymbols.byteArray)
    val charArray: BirClassSymbol = converter.remapSymbol(irSymbols.charArray)
    val shortArray: BirClassSymbol = converter.remapSymbol(irSymbols.shortArray)
    val intArray: BirClassSymbol = converter.remapSymbol(irSymbols.intArray)
    val longArray: BirClassSymbol = converter.remapSymbol(irSymbols.longArray)
    val floatArray: BirClassSymbol = converter.remapSymbol(irSymbols.floatArray)
    val doubleArray: BirClassSymbol = converter.remapSymbol(irSymbols.doubleArray)
    val booleanArray: BirClassSymbol = converter.remapSymbol(irSymbols.booleanArray)
    val byteArrayType: BirSimpleType = converter.remapSimpleType(irSymbols.byteArrayType)
    val charArrayType: BirSimpleType = converter.remapSimpleType(irSymbols.charArrayType)
    val shortArrayType: BirSimpleType = converter.remapSimpleType(irSymbols.shortArrayType)
    val intArrayType: BirSimpleType = converter.remapSimpleType(irSymbols.intArrayType)
    val longArrayType: BirSimpleType = converter.remapSimpleType(irSymbols.longArrayType)
    val floatArrayType: BirSimpleType = converter.remapSimpleType(irSymbols.floatArrayType)
    val doubleArrayType: BirSimpleType = converter.remapSimpleType(irSymbols.doubleArrayType)
    val booleanArrayType: BirSimpleType = converter.remapSimpleType(irSymbols.booleanArrayType)
    val primitiveTypesToPrimitiveArrays =
        irSymbols.primitiveTypesToPrimitiveArrays.mapValues { converter.remapSymbol<_, BirClassSymbol>(it.value) }
    val primitiveArraysToPrimitiveTypes =
        irSymbols.primitiveArraysToPrimitiveTypes.mapKeys { converter.remapSymbol<_, BirClassSymbol>(it.key) }
    val unsignedTypesToUnsignedArrays =
        irSymbols.unsignedTypesToUnsignedArrays.mapValues { converter.remapSymbol<_, BirClassSymbol>(it.value) }
    val arrays: List<BirClassSymbol> = irSymbols.arrays.map { converter.remapSymbol(it) }
    val collection: BirClassSymbol = converter.remapSymbol(irSymbols.collection)
    val set: BirClassSymbol = converter.remapSymbol(irSymbols.set)
    val list: BirClassSymbol = converter.remapSymbol(irSymbols.list)
    val map: BirClassSymbol = converter.remapSymbol(irSymbols.map)
    val mapEntry: BirClassSymbol = converter.remapSymbol(irSymbols.mapEntry)
    val iterable: BirClassSymbol = converter.remapSymbol(irSymbols.iterable)
    val listIterator: BirClassSymbol = converter.remapSymbol(irSymbols.listIterator)
    val mutableCollection: BirClassSymbol = converter.remapSymbol(irSymbols.mutableCollection)
    val mutableSet: BirClassSymbol = converter.remapSymbol(irSymbols.mutableSet)
    val mutableList: BirClassSymbol = converter.remapSymbol(irSymbols.mutableList)
    val mutableMap: BirClassSymbol = converter.remapSymbol(irSymbols.mutableMap)
    val mutableMapEntry: BirClassSymbol = converter.remapSymbol(irSymbols.mutableMapEntry)
    val mutableIterable: BirClassSymbol = converter.remapSymbol(irSymbols.mutableIterable)
    val mutableIterator: BirClassSymbol = converter.remapSymbol(irSymbols.mutableIterator)
    val mutableListIterator: BirClassSymbol = converter.remapSymbol(irSymbols.mutableListIterator)
    val comparable: BirClassSymbol = converter.remapSymbol(irSymbols.comparable)
}