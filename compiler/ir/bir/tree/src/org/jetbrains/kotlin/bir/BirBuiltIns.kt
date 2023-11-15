/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirProperty
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

@OptIn(ObsoleteDescriptorBasedAPI::class)
class BirBuiltIns(
    private val irBuiltIns: IrBuiltIns,
    private val converter: Ir2BirConverter,
) {
    val languageVersionSettings: LanguageVersionSettings = irBuiltIns.languageVersionSettings

    val anyType: BirType = converter.remapType(irBuiltIns.anyType)
    val anyClass: BirClass = remapSymbolOwner(irBuiltIns.anyClass)
    val anyNType: BirType = converter.remapType(irBuiltIns.anyNType)
    val booleanType: BirType = converter.remapType(irBuiltIns.booleanType)
    val booleanClass: BirClass = remapSymbolOwner(irBuiltIns.booleanClass)
    val charType: BirType = converter.remapType(irBuiltIns.charType)
    val charClass: BirClass = remapSymbolOwner(irBuiltIns.charClass)
    val numberType: BirType = converter.remapType(irBuiltIns.numberType)
    val numberClass: BirClass = remapSymbolOwner(irBuiltIns.numberClass)
    val byteType: BirType = converter.remapType(irBuiltIns.byteType)
    val byteClass: BirClass = remapSymbolOwner(irBuiltIns.byteClass)
    val shortType: BirType = converter.remapType(irBuiltIns.shortType)
    val shortClass: BirClass = remapSymbolOwner(irBuiltIns.shortClass)
    val intType: BirType = converter.remapType(irBuiltIns.intType)
    val intClass: BirClass = remapSymbolOwner(irBuiltIns.intClass)
    val longType: BirType = converter.remapType(irBuiltIns.longType)
    val longClass: BirClass = remapSymbolOwner(irBuiltIns.longClass)
    val floatType: BirType = converter.remapType(irBuiltIns.floatType)
    val floatClass: BirClass = remapSymbolOwner(irBuiltIns.floatClass)
    val doubleType: BirType = converter.remapType(irBuiltIns.doubleType)
    val doubleClass: BirClass = remapSymbolOwner(irBuiltIns.doubleClass)
    val nothingType: BirType = converter.remapType(irBuiltIns.nothingType)
    val nothingClass: BirClass = remapSymbolOwner(irBuiltIns.nothingClass)
    val nothingNType: BirType = converter.remapType(irBuiltIns.nothingNType)
    val unitType: BirType = converter.remapType(irBuiltIns.unitType)
    val unitClass: BirClass = remapSymbolOwner(irBuiltIns.unitClass)
    val stringType: BirType = converter.remapType(irBuiltIns.stringType)
    val stringClass: BirClass = remapSymbolOwner(irBuiltIns.stringClass)
    val charSequenceClass: BirClass = remapSymbolOwner(irBuiltIns.charSequenceClass)

    val collectionClass: BirClass = remapSymbolOwner(irBuiltIns.collectionClass)
    val arrayClass: BirClass = remapSymbolOwner(irBuiltIns.arrayClass)
    val setClass: BirClass = remapSymbolOwner(irBuiltIns.setClass)
    val listClass: BirClass = remapSymbolOwner(irBuiltIns.listClass)
    val mapClass: BirClass = remapSymbolOwner(irBuiltIns.mapClass)
    val mapEntryClass: BirClass = remapSymbolOwner(irBuiltIns.mapEntryClass)
    val iterableClass: BirClass = remapSymbolOwner(irBuiltIns.iterableClass)
    val iteratorClass: BirClass = remapSymbolOwner(irBuiltIns.iteratorClass)
    val listIteratorClass: BirClass = remapSymbolOwner(irBuiltIns.listIteratorClass)
    val mutableCollectionClass: BirClass = remapSymbolOwner(irBuiltIns.mutableCollectionClass)
    val mutableSetClass: BirClass = remapSymbolOwner(irBuiltIns.mutableSetClass)
    val mutableListClass: BirClass = remapSymbolOwner(irBuiltIns.mutableListClass)
    val mutableMapClass: BirClass = remapSymbolOwner(irBuiltIns.mutableMapClass)
    val mutableMapEntryClass: BirClass = remapSymbolOwner(irBuiltIns.mutableMapEntryClass)
    val mutableIterableClass: BirClass = remapSymbolOwner(irBuiltIns.mutableIterableClass)
    val mutableIteratorClass: BirClass = remapSymbolOwner(irBuiltIns.mutableIteratorClass)
    val mutableListIteratorClass: BirClass = remapSymbolOwner(irBuiltIns.mutableListIteratorClass)

    val comparableClass: BirClass = remapSymbolOwner(irBuiltIns.comparableClass)
    val throwableType: BirType = converter.remapType(irBuiltIns.throwableType)
    val throwableClass: BirClass = remapSymbolOwner(irBuiltIns.throwableClass)
    val kCallableClass: BirClass = remapSymbolOwner(irBuiltIns.kCallableClass)
    val kPropertyClass: BirClass = remapSymbolOwner(irBuiltIns.kPropertyClass)
    val kClassClass: BirClass = remapSymbolOwner(irBuiltIns.kClassClass)
    val kProperty0Class: BirClass = remapSymbolOwner(irBuiltIns.kProperty0Class)
    val kProperty1Class: BirClass = remapSymbolOwner(irBuiltIns.kProperty1Class)
    val kProperty2Class: BirClass = remapSymbolOwner(irBuiltIns.kProperty2Class)
    val kMutableProperty0Class: BirClass = remapSymbolOwner(irBuiltIns.kMutableProperty0Class)
    val kMutableProperty1Class: BirClass = remapSymbolOwner(irBuiltIns.kMutableProperty1Class)
    val kMutableProperty2Class: BirClass = remapSymbolOwner(irBuiltIns.kMutableProperty2Class)
    val functionClass: BirClass = remapSymbolOwner(irBuiltIns.functionClass)
    val kFunctionClass: BirClass = remapSymbolOwner(irBuiltIns.kFunctionClass)
    val annotationType: BirType = converter.remapType(irBuiltIns.annotationType)
    val annotationClass: BirClass = remapSymbolOwner(irBuiltIns.annotationClass)

    val primitiveBirTypes: List<BirType> = irBuiltIns.primitiveIrTypes.map { converter.remapType(it) }
    val primitiveBirTypesWithComparisons: List<BirType> = irBuiltIns.primitiveIrTypesWithComparisons.map { converter.remapType(it) }
    val primitiveFloatingPointBirTypes: List<BirType> = irBuiltIns.primitiveFloatingPointIrTypes.map { converter.remapType(it) }

    val byteArray: BirClass = remapSymbolOwner(irBuiltIns.byteArray)
    val charArray: BirClass = remapSymbolOwner(irBuiltIns.charArray)
    val shortArray: BirClass = remapSymbolOwner(irBuiltIns.shortArray)
    val intArray: BirClass = remapSymbolOwner(irBuiltIns.intArray)
    val longArray: BirClass = remapSymbolOwner(irBuiltIns.longArray)
    val floatArray: BirClass = remapSymbolOwner(irBuiltIns.floatArray)
    val doubleArray: BirClass = remapSymbolOwner(irBuiltIns.doubleArray)
    val booleanArray: BirClass = remapSymbolOwner(irBuiltIns.booleanArray)

    val primitiveArraysToPrimitiveTypes: Map<BirClass, PrimitiveType> =
        irBuiltIns.primitiveArraysToPrimitiveTypes.mapKeys { remapSymbolOwner(it.key) }
    val primitiveTypesToPrimitiveArrays: Map<PrimitiveType, BirClass> =
        irBuiltIns.primitiveTypesToPrimitiveArrays.mapValues { remapSymbolOwner(it.value) }
    val primitiveArrayElementTypes: Map<BirClass, BirType?> = irBuiltIns.primitiveArrayElementTypes.entries
        .associate { entry -> remapSymbolOwner<_, BirClass>(entry.key) to entry.value?.let { converter.remapType(it) } }
    val primitiveArrayForType: Map<BirType?, BirClass> = irBuiltIns.primitiveArrayForType.entries
        .associate { entry -> entry.key?.let { converter.remapType(it) } to remapSymbolOwner<_, BirClass>(entry.value) }

    val unsignedTypesToUnsignedArrays: Map<UnsignedType, BirClass> =
        irBuiltIns.unsignedTypesToUnsignedArrays.mapValues { remapSymbolOwner(it.value) }
    val unsignedArraysElementTypes: Map<BirClass, BirType?> = irBuiltIns.unsignedArraysElementTypes.entries
        .associate { entry -> remapSymbolOwner<_, BirClass>(entry.key) to entry.value?.let { converter.remapType(it) } }

    val lessFunByOperandType: Map<BirClass, BirSimpleFunction> = irBuiltIns.lessFunByOperandType.entries
        .associate { (key, value) -> remapSymbolOwner<_, BirClass>(key) to remapSymbolOwner(value) }
    val lessOrEqualFunByOperandType: Map<BirClass, BirSimpleFunction> = irBuiltIns.lessOrEqualFunByOperandType.entries
        .associate { (key, value) -> remapSymbolOwner<_, BirClass>(key) to remapSymbolOwner(value) }
    val greaterOrEqualFunByOperandType: Map<BirClass, BirSimpleFunction> = irBuiltIns.greaterOrEqualFunByOperandType.entries
        .associate { (key, value) -> remapSymbolOwner<_, BirClass>(key) to remapSymbolOwner(value) }
    val greaterFunByOperandType: Map<BirClass, BirSimpleFunction> = irBuiltIns.greaterFunByOperandType.entries
        .associate { (key, value) -> remapSymbolOwner<_, BirClass>(key) to remapSymbolOwner(value) }
    val ieee754equalsFunByOperandType: Map<BirClass, BirSimpleFunction> = irBuiltIns.ieee754equalsFunByOperandType.entries
        .associate { (key, value) -> remapSymbolOwner<_, BirClass>(key) to remapSymbolOwner(value) }

    val booleanNotSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.booleanNotSymbol)
    val eqeqeqSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.eqeqeqSymbol)
    val eqeqSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.eqeqSymbol)
    val throwCceSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.throwCceSymbol)
    val throwIseSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.throwIseSymbol)
    val andandSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.andandSymbol)
    val ororSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.ororSymbol)
    val noWhenBranchMatchedExceptionSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.noWhenBranchMatchedExceptionSymbol)
    val illegalArgumentExceptionSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.illegalArgumentExceptionSymbol)
    val checkNotNullSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.checkNotNullSymbol)
    val dataClassArrayMemberHashCodeSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.dataClassArrayMemberHashCodeSymbol)
    val dataClassArrayMemberToStringSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.dataClassArrayMemberToStringSymbol)
    val enumClass: BirClass = remapSymbolOwner(irBuiltIns.enumClass)

    val intPlusSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.intPlusSymbol)
    val intTimesSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.intTimesSymbol)
    val intXorSymbol: BirSimpleFunction = remapSymbolOwner(irBuiltIns.intXorSymbol)

    val extensionToString: BirSimpleFunction = remapSymbolOwner(irBuiltIns.extensionToString)
    val memberToString: BirSimpleFunction = remapSymbolOwner(irBuiltIns.memberToString)

    val extensionStringPlus: BirSimpleFunction = remapSymbolOwner(irBuiltIns.extensionStringPlus)
    val memberStringPlus: BirSimpleFunction = remapSymbolOwner(irBuiltIns.memberStringPlus)

    val arrayOf: BirSimpleFunction = remapSymbolOwner(irBuiltIns.arrayOf)
    val arrayOfNulls: BirSimpleFunction = remapSymbolOwner(irBuiltIns.arrayOfNulls)

    val lateinitIsInitialized: BirSimpleFunction? =
        irBuiltIns.findProperties(Name.identifier("isInitialized"), FqName("kotlin")).singleOrNull()?.takeIf { it.isBound }
            ?.let { remapSymbolOwner<_, BirProperty>(it).getter as BirSimpleFunction }

    val linkageErrorSymbol: BirSimpleFunction
        get() = TODO("TODO in IrBuiltInsOverFir")

    private val functionNCache = ConcurrentHashMap<IrClass, BirClass>()
    private val kFunctionNCache = ConcurrentHashMap<IrClass, BirClass>()
    private val suspendFunctionNCache = ConcurrentHashMap<IrClass, BirClass>()
    private val kSuspendFunctionNCache = ConcurrentHashMap<IrClass, BirClass>()
    fun functionN(arity: Int): BirClass = functionNCache.computeIfAbsent(irBuiltIns.functionN(arity)) { remapElement(it) }
    fun kFunctionN(arity: Int): BirClass = kFunctionNCache.computeIfAbsent(irBuiltIns.kFunctionN(arity)) { remapElement(it) }
    fun suspendFunctionN(arity: Int): BirClass =
        suspendFunctionNCache.computeIfAbsent(irBuiltIns.suspendFunctionN(arity)) { remapElement(it) }

    fun kSuspendFunctionN(arity: Int): BirClass =
        kSuspendFunctionNCache.computeIfAbsent(irBuiltIns.kSuspendFunctionN(arity)) { remapElement(it) }

    fun findFunctions(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): Iterable<BirSimpleFunction> {
        return irBuiltIns.findFunctions(name, *packageNameSegments).map { remapSymbolOwner(it) }
    }

    fun findClass(name: Name, packageFqName: FqName): BirClass? =
        irBuiltIns.findClass(name, packageFqName)
            ?.takeIf { it.isBound }
            ?.let { remapSymbolOwner(it) }

    fun findClass(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): BirClass? =
        irBuiltIns.findClass(name, *packageNameSegments)
            ?.takeIf { it.isBound }
            ?.let { remapSymbolOwner(it) }

    fun findClass(fqName: FqName): BirClass? =
        findClass(fqName.shortName(), fqName.parent())

    /*private fun <IrS : IrSymbol, BirS : BirSymbol> mapSymbolToOwner(symbol: IrS): BirS {
        return converter.mapSymbolToOwner(symbol.owner, symbol)
    }*/
    private fun <Ir : IrElement, Bir : BirElement> remapElement(element: Ir): Bir {
        return converter.remapElement(element)
    }

    private fun <IrS : IrSymbol, Bir : BirElement> remapSymbolOwner(symbol: IrS): Bir {
        return remapElement(symbol.owner)
    }
}