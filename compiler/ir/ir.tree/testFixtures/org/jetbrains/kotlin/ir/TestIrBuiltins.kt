/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.ir.TestIrBuiltins.missingBuiltIn
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KProperty

/**
 * A test implementation of [IrBuiltIns] that provides minimal built-in types and classes
 * for unit testing purposes. Most operations that are not commonly needed in tests
 * will throw an error via [missingBuiltIn].
 */
object TestIrBuiltins : IrBuiltIns() {
    override val symbolFinder by lazy { missingBuiltIn() }

    private val builtinsPackage = IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(), FqName("kotlin"))

    override val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT

    override val irFactory: IrFactory
        get() = IrFactoryImpl

    override val anyClass: IrClassSymbol by builtinClass("Any")
    override val anyType: IrType by builtinType(anyClass)
    override val anyNType: IrType by builtinType(anyClass, nullable = true)
    override val booleanClass: IrClassSymbol by builtinClass("Boolean")
    override val booleanType: IrType by builtinType(booleanClass)
    override val charClass: IrClassSymbol by builtinClass("Char")
    override val charType: IrType by builtinType(charClass)
    override val numberClass: IrClassSymbol by builtinClass("Number")
    override val numberType: IrType by builtinType(numberClass)
    override val byteClass: IrClassSymbol by builtinClass("Byte")
    override val byteType: IrType by builtinType(byteClass)
    override val shortClass: IrClassSymbol by builtinClass("Short")
    override val shortType: IrType by builtinType(shortClass)
    override val intClass: IrClassSymbol by builtinClass("Int")
    override val intType: IrType by builtinType(intClass)
    override val longClass: IrClassSymbol by builtinClass("Long")
    override val longType: IrType by builtinType(longClass)
    override val floatClass: IrClassSymbol by builtinClass("Float")
    override val floatType: IrType by builtinType(floatClass)
    override val doubleClass: IrClassSymbol by builtinClass("Double")
    override val doubleType: IrType by builtinType(doubleClass)
    override val nothingClass: IrClassSymbol by builtinClass("Nothing")
    override val nothingType: IrType by builtinType(nothingClass)
    override val nothingNType: IrType by builtinType(nothingClass)
    override val unitClass: IrClassSymbol by builtinClass("Unit")
    override val unitType: IrType by builtinType(unitClass)
    override val stringClass: IrClassSymbol by builtinClass("String")
    override val stringType: IrType by builtinType(stringClass)
    override val charSequenceClass: IrClassSymbol by builtinClass("CharSequence")
    override val collectionClass: IrClassSymbol by builtinClass("Collection")
    override val arrayClass: IrClassSymbol by builtinClass("Array")
    override val setClass: IrClassSymbol by builtinClass("Set")
    override val listClass: IrClassSymbol by builtinClass("List")
    override val mapClass: IrClassSymbol by builtinClass("Map")
    override val mapEntryClass: IrClassSymbol by builtinClass("Entry")
    override val iterableClass: IrClassSymbol by builtinClass("Iterable")
    override val iteratorClass: IrClassSymbol by builtinClass("Iterator")
    override val listIteratorClass: IrClassSymbol by builtinClass("ListIterator")
    override val mutableCollectionClass: IrClassSymbol by builtinClass("MutableCollection")
    override val mutableSetClass: IrClassSymbol by builtinClass("MutableSet")
    override val mutableListClass: IrClassSymbol by builtinClass("MutableList")
    override val mutableMapClass: IrClassSymbol by builtinClass("MutableMap")
    override val mutableMapEntryClass: IrClassSymbol by builtinClass("Entry")
    override val mutableIterableClass: IrClassSymbol by builtinClass("MutableIterable")
    override val mutableIteratorClass: IrClassSymbol by builtinClass("MutableIterator")
    override val mutableListIteratorClass: IrClassSymbol by builtinClass("MutableListIterator")
    override val comparableClass: IrClassSymbol by builtinClass("Comparable")
    override val throwableClass: IrClassSymbol by builtinClass("Throwable")
    override val throwableType: IrType by builtinType(throwableClass)
    override val kCallableClass: IrClassSymbol by builtinClass("KCallable")
    override val kPropertyClass: IrClassSymbol by builtinClass("KProperty")
    override val kClassClass: IrClassSymbol by builtinClass("KClass")
    override val kTypeClass: IrClassSymbol by builtinClass("KType")
    override val kProperty0Class: IrClassSymbol by builtinClass("KProperty0")
    override val kProperty1Class: IrClassSymbol by builtinClass("KProperty1")
    override val kProperty2Class: IrClassSymbol by builtinClass("KProperty2")
    override val kMutableProperty0Class: IrClassSymbol by builtinClass("KMutableProperty0")
    override val kMutableProperty1Class: IrClassSymbol by builtinClass("KMutableProperty1")
    override val kMutableProperty2Class: IrClassSymbol by builtinClass("KMutableProperty2")
    override val functionClass: IrClassSymbol by builtinClass("Function")
    override val kFunctionClass: IrClassSymbol by builtinClass("KFunction")
    override val annotationClass: IrClassSymbol by builtinClass("Annotation")
    override val annotationType: IrType by builtinType(annotationClass)
    override val ubyteClass: IrClassSymbol by builtinClass("UByte")
    override val ubyteType: IrType by builtinType(ubyteClass)
    override val ushortClass: IrClassSymbol by builtinClass("UShort")
    override val ushortType: IrType by builtinType(ushortClass)
    override val uintClass: IrClassSymbol by builtinClass("UInt")
    override val uintType: IrType by builtinType(uintClass)
    override val ulongClass: IrClassSymbol by builtinClass("ULong")
    override val ulongType: IrType by builtinType(ulongClass)

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

    override val primitiveIrTypes: List<IrType>
        get() = missingBuiltIn()
    override val primitiveIrTypesWithComparisons: List<IrType>
        get() = missingBuiltIn()
    override val primitiveFloatingPointIrTypes: List<IrType>
        get() = missingBuiltIn()

    override val byteIterator: IrClassSymbol by builtinClass("ByteIterator")
    override val charIterator: IrClassSymbol by builtinClass("CharIterator")
    override val shortIterator: IrClassSymbol by builtinClass("ShortIterator")
    override val intIterator: IrClassSymbol by builtinClass("IntIterator")
    override val longIterator: IrClassSymbol by builtinClass("LongIterator")
    override val floatIterator: IrClassSymbol by builtinClass("FloatIterator")
    override val doubleIterator: IrClassSymbol by builtinClass("DoubleIterator")
    override val booleanIterator: IrClassSymbol by builtinClass("BooleanIterator")
    override val byteArray: IrClassSymbol by builtinClass("ByteArray")
    override val charArray: IrClassSymbol by builtinClass("CharArray")
    override val shortArray: IrClassSymbol by builtinClass("ShortArray")
    override val intArray: IrClassSymbol by builtinClass("IntArray")
    override val longArray: IrClassSymbol by builtinClass("LongArray")
    override val floatArray: IrClassSymbol by builtinClass("FloatArray")
    override val doubleArray: IrClassSymbol by builtinClass("DoubleArray")
    override val booleanArray: IrClassSymbol by builtinClass("BooleanArray")
    override val ubyteArray: IrClassSymbol by builtinClass("UByteArray")
    override val ushortArray: IrClassSymbol by builtinClass("UShortArray")
    override val uintArray: IrClassSymbol by builtinClass("UIntArray")
    override val ulongArray: IrClassSymbol by builtinClass("ULongArray")
    val booleanArrayType: IrType by builtinType(booleanArray)
    val array: IrClassSymbol by builtinClass("Array")
    val arrayOfStringType: IrType by builtinType(array, listOf(stringType))

    override val primitiveArraysToPrimitiveTypes: Map<IrClassSymbol, PrimitiveType>
        get() = missingBuiltIn()
    override val primitiveTypesToPrimitiveArrays: Map<PrimitiveType, IrClassSymbol>
        get() = missingBuiltIn()
    override val primitiveArrayElementTypes: Map<IrClassSymbol, IrType?>
        get() = mapOf(
            booleanArray to booleanType,
            charArray to charType,
            byteArray to byteType,
            shortArray to shortType,
            intArray to intType,
            longArray to longType,
            floatArray to floatType,
            doubleArray to doubleType,
        )
    override val primitiveArrayForType: Map<IrType?, IrClassSymbol>
        get() = missingBuiltIn()
    override val unsignedTypesToUnsignedArrays: Map<UnsignedType, IrClassSymbol>
        get() = missingBuiltIn()
    override val unsignedArraysElementTypes: Map<IrClassSymbol, IrType?>
        get() = mapOf(
            ubyteArray to ubyteType,
            ushortArray to ushortType,
            uintArray to uintType,
            ulongArray to ulongType,
        )
    override val lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = missingBuiltIn()
    override val lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = missingBuiltIn()
    override val greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = missingBuiltIn()
    override val greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = missingBuiltIn()
    override val ieee754equalsFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = missingBuiltIn()
    override val booleanNotSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val eqeqeqSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val eqeqSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val throwCceSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val throwIseSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val andandSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val ororSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val checkNotNullSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()

    override val enumClass: IrClassSymbol by builtinClass("Enum")

    override val intPlusSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val intTimesSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val intXorSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val intAndSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val arrayOf: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val arrayOfNulls: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val linkageErrorSymbol: IrSimpleFunctionSymbol
        get() = missingBuiltIn()
    override val deprecatedSymbol: IrClassSymbol
        get() = missingBuiltIn()
    override val deprecationLevelSymbol: IrClassSymbol
        get() = missingBuiltIn()

    override fun functionN(arity: Int): IrClass {
        missingBuiltIn()
    }

    override fun kFunctionN(arity: Int): IrClass {
        missingBuiltIn()
    }

    override fun suspendFunctionN(arity: Int): IrClass {
        missingBuiltIn()
    }

    override fun kSuspendFunctionN(arity: Int): IrClass {
        missingBuiltIn()
    }

    override fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol {
        missingBuiltIn()
    }

    override val operatorsPackageFragment: IrExternalPackageFragment
        get() = missingBuiltIn()
    override val kotlinInternalPackageFragment: IrExternalPackageFragment
        get() = missingBuiltIn()

    private fun builtinClass(name: String) = object {
        val klass = irFactory.buildClass {
            this.name = Name.identifier(name)
        }.apply {
            parent = builtinsPackage
        }

        operator fun getValue(thisRef: TestIrBuiltins, property: KProperty<*>): IrClassSymbol {
            return klass.symbol
        }
    }

    private fun builtinType(klass: IrClassSymbol, nullable: Boolean = false) = object {
        val type = IrSimpleTypeImpl(klass, SimpleTypeNullability.fromHasQuestionMark(nullable), emptyList(), emptyList())
        operator fun getValue(thisRef: TestIrBuiltins, property: KProperty<*>): IrType {
            return type
        }
    }

    private fun builtinType(klass: IrClassSymbol, arguments: List<IrTypeArgument>, nullable: Boolean = false) = object {
        val type = IrSimpleTypeImpl(klass, SimpleTypeNullability.fromHasQuestionMark(nullable), arguments, emptyList())
        operator fun getValue(thisRef: TestIrBuiltins, property: KProperty<*>): IrType {
            return type
        }
    }

    private fun missingBuiltIn(): Nothing = error("Missing built-in")
}
