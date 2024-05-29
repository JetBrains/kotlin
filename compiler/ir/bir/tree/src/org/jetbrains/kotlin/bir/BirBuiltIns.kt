/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirSymbolOwner
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.symbols.BirSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

class BirBuiltIns(
    private val irBuiltIns: IrBuiltIns,
    private val converter: Ir2BirConverter,
) {
    val languageVersionSettings: LanguageVersionSettings = irBuiltIns.languageVersionSettings

    val anyType: BirType = converter.remapType(irBuiltIns.anyType)
    val anyClass: BirClassSymbol = remapSymbol(irBuiltIns.anyClass)
    val anyNType: BirType = converter.remapType(irBuiltIns.anyNType)
    val booleanType: BirType = converter.remapType(irBuiltIns.booleanType)
    val booleanClass: BirClassSymbol = remapSymbol(irBuiltIns.booleanClass)
    val charType: BirType = converter.remapType(irBuiltIns.charType)
    val charClass: BirClassSymbol = remapSymbol(irBuiltIns.charClass)
    val numberType: BirType = converter.remapType(irBuiltIns.numberType)
    val numberClass: BirClassSymbol = remapSymbol(irBuiltIns.numberClass)
    val byteType: BirType = converter.remapType(irBuiltIns.byteType)
    val byteClass: BirClassSymbol = remapSymbol(irBuiltIns.byteClass)
    val shortType: BirType = converter.remapType(irBuiltIns.shortType)
    val shortClass: BirClassSymbol = remapSymbol(irBuiltIns.shortClass)
    val intType: BirType = converter.remapType(irBuiltIns.intType)
    val intClass: BirClassSymbol = remapSymbol(irBuiltIns.intClass)
    val longType: BirType = converter.remapType(irBuiltIns.longType)
    val longClass: BirClassSymbol = remapSymbol(irBuiltIns.longClass)
    val floatType: BirType = converter.remapType(irBuiltIns.floatType)
    val floatClass: BirClassSymbol = remapSymbol(irBuiltIns.floatClass)
    val doubleType: BirType = converter.remapType(irBuiltIns.doubleType)
    val doubleClass: BirClassSymbol = remapSymbol(irBuiltIns.doubleClass)
    val nothingType: BirType = converter.remapType(irBuiltIns.nothingType)
    val nothingClass: BirClassSymbol = remapSymbol(irBuiltIns.nothingClass)
    val nothingNType: BirType = converter.remapType(irBuiltIns.nothingNType)
    val unitType: BirType = converter.remapType(irBuiltIns.unitType)
    val unitClass: BirClassSymbol = remapSymbol(irBuiltIns.unitClass)
    val stringType: BirType = converter.remapType(irBuiltIns.stringType)
    val stringClass: BirClassSymbol = remapSymbol(irBuiltIns.stringClass)
    val charSequenceClass: BirClassSymbol = remapSymbol(irBuiltIns.charSequenceClass)

    val collectionClass: BirClassSymbol = remapSymbol(irBuiltIns.collectionClass)
    val arrayClass: BirClassSymbol = remapSymbol(irBuiltIns.arrayClass)
    val setClass: BirClassSymbol = remapSymbol(irBuiltIns.setClass)
    val listClass: BirClassSymbol = remapSymbol(irBuiltIns.listClass)
    val mapClass: BirClassSymbol = remapSymbol(irBuiltIns.mapClass)
    val mapEntryClass: BirClassSymbol = remapSymbol(irBuiltIns.mapEntryClass)
    val iterableClass: BirClassSymbol = remapSymbol(irBuiltIns.iterableClass)
    val iteratorClass: BirClassSymbol = remapSymbol(irBuiltIns.iteratorClass)
    val listIteratorClass: BirClassSymbol = remapSymbol(irBuiltIns.listIteratorClass)
    val mutableCollectionClass: BirClassSymbol = remapSymbol(irBuiltIns.mutableCollectionClass)
    val mutableSetClass: BirClassSymbol = remapSymbol(irBuiltIns.mutableSetClass)
    val mutableListClass: BirClassSymbol = remapSymbol(irBuiltIns.mutableListClass)
    val mutableMapClass: BirClassSymbol = remapSymbol(irBuiltIns.mutableMapClass)
    val mutableMapEntryClass: BirClassSymbol = remapSymbol(irBuiltIns.mutableMapEntryClass)
    val mutableIterableClass: BirClassSymbol = remapSymbol(irBuiltIns.mutableIterableClass)
    val mutableIteratorClass: BirClassSymbol = remapSymbol(irBuiltIns.mutableIteratorClass)
    val mutableListIteratorClass: BirClassSymbol = remapSymbol(irBuiltIns.mutableListIteratorClass)

    val comparableClass: BirClassSymbol = remapSymbol(irBuiltIns.comparableClass)
    val throwableType: BirType = converter.remapType(irBuiltIns.throwableType)
    val throwableClass: BirClassSymbol = remapSymbol(irBuiltIns.throwableClass)
    val kCallableClass: BirClassSymbol = remapSymbol(irBuiltIns.kCallableClass)
    val kPropertyClass: BirClassSymbol = remapSymbol(irBuiltIns.kPropertyClass)
    val kClassClass: BirClassSymbol = remapSymbol(irBuiltIns.kClassClass)
    val kProperty0Class: BirClassSymbol = remapSymbol(irBuiltIns.kProperty0Class)
    val kProperty1Class: BirClassSymbol = remapSymbol(irBuiltIns.kProperty1Class)
    val kProperty2Class: BirClassSymbol = remapSymbol(irBuiltIns.kProperty2Class)
    val kMutableProperty0Class: BirClassSymbol = remapSymbol(irBuiltIns.kMutableProperty0Class)
    val kMutableProperty1Class: BirClassSymbol = remapSymbol(irBuiltIns.kMutableProperty1Class)
    val kMutableProperty2Class: BirClassSymbol = remapSymbol(irBuiltIns.kMutableProperty2Class)
    val functionClass: BirClassSymbol = remapSymbol(irBuiltIns.functionClass)
    val kFunctionClass: BirClassSymbol = remapSymbol(irBuiltIns.kFunctionClass)
    val annotationType: BirType = converter.remapType(irBuiltIns.annotationType)
    val annotationClass: BirClassSymbol = remapSymbol(irBuiltIns.annotationClass)

    val primitiveBirTypes: List<BirType> = irBuiltIns.primitiveIrTypes.map { converter.remapType(it) }
    val primitiveBirTypesWithComparisons: List<BirType> = irBuiltIns.primitiveIrTypesWithComparisons.map { converter.remapType(it) }
    val primitiveFloatingPointBirTypes: List<BirType> = irBuiltIns.primitiveFloatingPointIrTypes.map { converter.remapType(it) }

    val byteArray: BirClassSymbol = remapSymbol(irBuiltIns.byteArray)
    val charArray: BirClassSymbol = remapSymbol(irBuiltIns.charArray)
    val shortArray: BirClassSymbol = remapSymbol(irBuiltIns.shortArray)
    val intArray: BirClassSymbol = remapSymbol(irBuiltIns.intArray)
    val longArray: BirClassSymbol = remapSymbol(irBuiltIns.longArray)
    val floatArray: BirClassSymbol = remapSymbol(irBuiltIns.floatArray)
    val doubleArray: BirClassSymbol = remapSymbol(irBuiltIns.doubleArray)
    val booleanArray: BirClassSymbol = remapSymbol(irBuiltIns.booleanArray)

    val primitiveArraysToPrimitiveTypes: Map<BirClassSymbol, PrimitiveType> =
        irBuiltIns.primitiveArraysToPrimitiveTypes.mapKeys { remapSymbol(it.key) }
    val primitiveTypesToPrimitiveArrays: Map<PrimitiveType, BirClassSymbol> =
        irBuiltIns.primitiveTypesToPrimitiveArrays.mapValues { remapSymbol(it.value) }
    val primitiveArrayElementTypes: Map<BirClassSymbol, BirType?> = irBuiltIns.primitiveArrayElementTypes.entries
        .associate { entry -> remapSymbol<_, BirClassSymbol>(entry.key) to entry.value?.let { converter.remapType(it) } }
    val primitiveArrayForType: Map<BirType?, BirClassSymbol> = irBuiltIns.primitiveArrayForType.entries
        .associate { entry -> entry.key?.let { converter.remapType(it) } to remapSymbol<_, BirClassSymbol>(entry.value) }

    val unsignedTypesToUnsignedArrays: Map<UnsignedType, BirClassSymbol> =
        irBuiltIns.unsignedTypesToUnsignedArrays.mapValues { remapSymbol(it.value) }
    val unsignedArraysElementTypes: Map<BirClassSymbol, BirType?> = irBuiltIns.unsignedArraysElementTypes.entries
        .associate { entry -> remapSymbol<_, BirClassSymbol>(entry.key) to entry.value?.let { converter.remapType(it) } }

    val lessFunByOperandType: Map<BirClassSymbol, BirSimpleFunctionSymbol> = irBuiltIns.lessFunByOperandType.entries
        .associate { (key, value) -> remapSymbol<_, BirClassSymbol>(key) to remapSymbol(value) }
    val lessOrEqualFunByOperandType: Map<BirClassSymbol, BirSimpleFunctionSymbol> = irBuiltIns.lessOrEqualFunByOperandType.entries
        .associate { (key, value) -> remapSymbol<_, BirClassSymbol>(key) to remapSymbol(value) }
    val greaterOrEqualFunByOperandType: Map<BirClassSymbol, BirSimpleFunctionSymbol> = irBuiltIns.greaterOrEqualFunByOperandType.entries
        .associate { (key, value) -> remapSymbol<_, BirClassSymbol>(key) to remapSymbol(value) }
    val greaterFunByOperandType: Map<BirClassSymbol, BirSimpleFunctionSymbol> = irBuiltIns.greaterFunByOperandType.entries
        .associate { (key, value) -> remapSymbol<_, BirClassSymbol>(key) to remapSymbol(value) }
    val ieee754equalsFunByOperandType: Map<BirClassSymbol, BirSimpleFunctionSymbol> = irBuiltIns.ieee754equalsFunByOperandType.entries
        .associate { (key, value) -> remapSymbol<_, BirClassSymbol>(key) to remapSymbol(value) }

    val booleanNotSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.booleanNotSymbol)
    val eqeqeqSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.eqeqeqSymbol)
    val eqeqSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.eqeqSymbol)
    val throwCceSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.throwCceSymbol)
    val throwIseSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.throwIseSymbol)
    val andandSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.andandSymbol)
    val ororSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.ororSymbol)
    val noWhenBranchMatchedExceptionSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.noWhenBranchMatchedExceptionSymbol)
    val illegalArgumentExceptionSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.illegalArgumentExceptionSymbol)
    val checkNotNullSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.checkNotNullSymbol)
    val dataClassArrayMemberHashCodeSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.dataClassArrayMemberHashCodeSymbol)
    val dataClassArrayMemberToStringSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.dataClassArrayMemberToStringSymbol)
    val enumClass: BirClassSymbol = remapSymbol(irBuiltIns.enumClass)

    val intPlusSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.intPlusSymbol)
    val intTimesSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.intTimesSymbol)
    val intXorSymbol: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.intXorSymbol)

    val extensionToString: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.extensionToString)
    val memberToString: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.memberToString)

    val extensionStringPlus: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.extensionStringPlus)
    val memberStringPlus: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.memberStringPlus)

    val arrayOf: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.arrayOf)
    val arrayOfNulls: BirSimpleFunctionSymbol = remapSymbol(irBuiltIns.arrayOfNulls)

    val lateinitIsInitialized: BirSimpleFunctionSymbol? =
        irBuiltIns.findProperties(Name.identifier("isInitialized"), FqName("kotlin")).singleOrNull()?.takeIf { it.isBound }
            ?.let { remapSymbol<_, BirPropertySymbol>(it).owner.getter as BirSimpleFunctionSymbol }

    val linkageErrorSymbol: BirSimpleFunctionSymbol
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

    fun findFunctions(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): Iterable<BirSimpleFunctionSymbol> {
        return irBuiltIns.findFunctions(name, *packageNameSegments).map { remapSymbol(it) }
    }

    fun findClass(name: Name, packageFqName: FqName): BirClassSymbol? =
        irBuiltIns.findClass(name, packageFqName)
            ?.takeIf { it.isBound }
            ?.let { remapSymbol(it) }

    fun findClass(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): BirClass? =
        irBuiltIns.findClass(name, *packageNameSegments)
            ?.takeIf { it.isBound }
            ?.let { remapSymbol(it) }

    fun findClass(fqName: FqName): BirClassSymbol? =
        findClass(fqName.shortName(), fqName.parent())

    private fun <Ir : IrSymbolOwner, Bir : BirSymbolOwner> remapElement(element: Ir): Bir {
        return converter.remapElement(element)
    }

    private fun <IrS : IrSymbol, BirS : BirSymbol> remapSymbol(symbol: IrS): BirS {
        @Suppress("UNCHECKED_CAST")
        return remapElement<IrSymbolOwner, BirSymbolOwner>(symbol.owner).symbol as BirS
    }
}