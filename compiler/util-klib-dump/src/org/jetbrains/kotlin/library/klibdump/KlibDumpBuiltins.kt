/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.klibdump

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class KlibDumpBuiltins(override val languageVersionSettings: LanguageVersionSettings) : IrBuiltIns() {

    override val irFactory: IrFactory
        get() = IrFactoryImpl

    override val anyType: IrType
        get() = TODO("Not yet implemented")

    override val anyClass: IrClassSymbol = IrClassSymbolImpl()

    override val anyNType: IrType
        get() = TODO("Not yet implemented")

    override val booleanType: IrType
        get() = TODO("Not yet implemented")

    override val booleanClass: IrClassSymbol = IrClassSymbolImpl()

    override val charType: IrType
        get() = TODO("Not yet implemented")

    override val charClass: IrClassSymbol = IrClassSymbolImpl()

    override val numberType: IrType
        get() = TODO("Not yet implemented")

    override val numberClass: IrClassSymbol = IrClassSymbolImpl()

    override val byteType: IrType
        get() = TODO("Not yet implemented")

    override val byteClass: IrClassSymbol = IrClassSymbolImpl()

    override val shortType: IrType
        get() = TODO("Not yet implemented")

    override val shortClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val intType: IrType
        get() = TODO("Not yet implemented")

    override val intClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val longType: IrType
        get() = TODO("Not yet implemented")

    override val longClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val floatType: IrType
        get() = TODO("Not yet implemented")

    override val floatClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val doubleType: IrType
        get() = TODO("Not yet implemented")

    override val doubleClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val nothingType: IrType
        get() = TODO("Not yet implemented")

    override val nothingClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val nothingNType: IrType
        get() = TODO("Not yet implemented")

    override val unitType: IrType
        get() = TODO("Not yet implemented")

    override val unitClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val stringType: IrType
        get() = TODO("Not yet implemented")

    override val stringClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val charSequenceClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val collectionClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val arrayClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val setClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val listClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val mapClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val mapEntryClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val iterableClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val iteratorClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val listIteratorClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val mutableCollectionClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val mutableSetClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val mutableListClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val mutableMapClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val mutableMapEntryClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val mutableIterableClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val mutableIteratorClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val mutableListIteratorClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val comparableClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val throwableType: IrType
        get() = TODO("Not yet implemented")

    override val throwableClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val kCallableClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val kPropertyClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val kClassClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val kProperty0Class: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val kProperty1Class: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val kProperty2Class: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val kMutableProperty0Class: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val kMutableProperty1Class: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val kMutableProperty2Class: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val functionClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val kFunctionClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val annotationType: IrType
        get() = TODO("Not yet implemented")

    override val annotationClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val primitiveTypeToIrType: Map<PrimitiveType, IrType>
        get() = TODO("Not yet implemented")

    override val primitiveIrTypes: List<IrType>
        get() = TODO("Not yet implemented")

    override val primitiveIrTypesWithComparisons: List<IrType>
        get() = TODO("Not yet implemented")

    override val primitiveFloatingPointIrTypes: List<IrType>
        get() = TODO("Not yet implemented")

    override val byteArray: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val charArray: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val shortArray: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val intArray: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val longArray: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val floatArray: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val doubleArray: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val booleanArray: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val primitiveArraysToPrimitiveTypes: Map<IrClassSymbol, PrimitiveType>
        get() = TODO("Not yet implemented")

    override val primitiveTypesToPrimitiveArrays: Map<PrimitiveType, IrClassSymbol>
        get() = TODO("Not yet implemented")

    override val primitiveArrayElementTypes: Map<IrClassSymbol, IrType?>
        get() = TODO("Not yet implemented")

    override val primitiveArrayForType: Map<IrType?, IrClassSymbol>
        get() = TODO("Not yet implemented")

    override val unsignedTypesToUnsignedArrays: Map<UnsignedType, IrClassSymbol>
        get() = TODO("Not yet implemented")

    override val lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = TODO("Not yet implemented")

    override val lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = TODO("Not yet implemented")

    override val greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = TODO("Not yet implemented")

    override val greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = TODO("Not yet implemented")

    override val ieee754equalsFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = TODO("Not yet implemented")

    override val booleanNotSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val eqeqeqSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val eqeqSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val throwCceSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val throwIseSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val andandSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val ororSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val checkNotNullSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val enumClass: IrClassSymbol
        get() = TODO("Not yet implemented")

    override val intPlusSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val intTimesSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val intXorSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val extensionToString: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val memberToString: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val extensionStringPlus: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val memberStringPlus: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val arrayOf: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val arrayOfNulls: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val linkageErrorSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override fun functionN(arity: Int): IrClass {
        TODO("Not yet implemented")
    }

    override fun kFunctionN(arity: Int): IrClass {
        TODO("Not yet implemented")
    }

    override fun suspendFunctionN(arity: Int): IrClass {
        TODO("Not yet implemented")
    }

    override fun kSuspendFunctionN(arity: Int): IrClass {
        TODO("Not yet implemented")
    }

    override fun findFunctions(name: Name, vararg packageNameSegments: String): Iterable<IrSimpleFunctionSymbol> {
        TODO("Not yet implemented")
    }

    override fun findFunctions(name: Name, packageFqName: FqName): Iterable<IrSimpleFunctionSymbol> {
        TODO("Not yet implemented")
    }

    override fun findClass(name: Name, vararg packageNameSegments: String): IrClassSymbol? {
        TODO("Not yet implemented")
    }

    override fun findClass(name: Name, packageFqName: FqName): IrClassSymbol? {
        TODO("Not yet implemented")
    }

    override fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol {
        TODO("Not yet implemented")
    }

    override fun findBuiltInClassMemberFunctions(builtInClass: IrClassSymbol, name: Name): Iterable<IrSimpleFunctionSymbol> {
        TODO("Not yet implemented")
    }

    override fun getNonBuiltInFunctionsByExtensionReceiver(
        name: Name,
        vararg packageNameSegments: String
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> {
        TODO("Not yet implemented")
    }

    override fun getNonBuiltinFunctionsByReturnType(
        name: Name,
        vararg packageNameSegments: String
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> {
        TODO("Not yet implemented")
    }

    override fun getBinaryOperator(name: Name, lhsType: IrType, rhsType: IrType): IrSimpleFunctionSymbol {
        TODO("Not yet implemented")
    }

    override fun getUnaryOperator(name: Name, receiverType: IrType): IrSimpleFunctionSymbol {
        TODO("Not yet implemented")
    }

    override val operatorsPackageFragment: IrExternalPackageFragment
        get() = TODO("Not yet implemented")
}
