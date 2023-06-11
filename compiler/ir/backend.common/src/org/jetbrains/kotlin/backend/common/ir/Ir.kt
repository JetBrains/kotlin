/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

// This is what Context collects about IR.
abstract class Ir<out T : CommonBackendContext>(val context: T) {

    abstract val symbols: Symbols

    internal val localScopeWithCounterMap = LocalDeclarationsLowering.LocalScopeWithCounterMap()

    open fun shouldGenerateHandlerParameterForDefaultBodyFun() = false
}

open class BuiltinSymbolsBase(val irBuiltIns: IrBuiltIns, private val symbolTable: ReferenceSymbolTable) {

    private fun getClass(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): IrClassSymbol =
        irBuiltIns.findClass(name, *packageNameSegments)
            ?: error("Class '$name' not found in package '${packageNameSegments.joinToString(".")}'")

    /**
     * Use this table to reference external dependencies.
     */
    open val externalSymbolTable: ReferenceSymbolTable
        get() = symbolTable

    val iterator = getClass(Name.identifier("Iterator"), "kotlin", "collections")

    val charSequence = getClass(Name.identifier("CharSequence"), "kotlin")
    val string = getClass(Name.identifier("String"), "kotlin")

    val primitiveIteratorsByType = PrimitiveType.values().associate { type ->
        val iteratorClass = getClass(Name.identifier(type.typeName.asString() + "Iterator"), "kotlin", "collections")
        type to iteratorClass
    }

    val asserts = irBuiltIns.findFunctions(Name.identifier("assert"), "kotlin")

    private fun progression(name: String) = getClass(Name.identifier(name), "kotlin", "ranges")
    private fun progressionOrNull(name: String) = irBuiltIns.findClass(Name.identifier(name), "kotlin", "ranges")

    // The "...OrNull" variants are used for the classes below because the minimal stdlib used in tests do not include those classes.
    // It was not feasible to add them to the JS reduced runtime because all its transitive dependencies also need to be
    // added, which would include a lot of the full stdlib.
    val uByte = irBuiltIns.findClass(Name.identifier("UByte"), "kotlin")
    val uShort = irBuiltIns.findClass(Name.identifier("UShort"), "kotlin")
    val uInt = irBuiltIns.findClass(Name.identifier("UInt"), "kotlin")
    val uLong = irBuiltIns.findClass(Name.identifier("ULong"), "kotlin")
    val uIntProgression = progressionOrNull("UIntProgression")
    val uLongProgression = progressionOrNull("ULongProgression")
    val uIntRange = progressionOrNull("UIntRange")
    val uLongRange = progressionOrNull("ULongRange")
    val sequence = irBuiltIns.findClass(Name.identifier("Sequence"), "kotlin", "sequences")

    val charProgression = progression("CharProgression")
    val intProgression = progression("IntProgression")
    val longProgression = progression("LongProgression")
    val progressionClasses = listOfNotNull(charProgression, intProgression, longProgression, uIntProgression, uLongProgression)

    val charRange = progression("CharRange")
    val intRange = progression("IntRange")
    val longRange = progression("LongRange")
    val rangeClasses = listOfNotNull(charRange, intRange, longRange, uIntRange, uLongRange)

    val closedRange = progression("ClosedRange")

    open val getProgressionLastElementByReturnType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        irBuiltIns.getNonBuiltinFunctionsByReturnType(Name.identifier("getProgressionLastElement"), "kotlin", "internal")

    open val toUIntByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        irBuiltIns.getNonBuiltInFunctionsByExtensionReceiver(Name.identifier("toUInt"), "kotlin")

    open val toULongByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        irBuiltIns.getNonBuiltInFunctionsByExtensionReceiver(Name.identifier("toULong"), "kotlin")

    val any get() = irBuiltIns.anyClass
    val unit get() = irBuiltIns.unitClass

    val char get() = irBuiltIns.charClass

    val byte get() = irBuiltIns.byteClass
    val short get() = irBuiltIns.shortClass
    val int get() = irBuiltIns.intClass
    val long get() = irBuiltIns.longClass
    val float get() = irBuiltIns.floatClass
    val double get() = irBuiltIns.doubleClass

    val integerClasses = listOf(byte, short, int, long)

    val progressionElementTypes: Collection<IrType> by lazy {
        listOfNotNull(byte, short, int, long, char, uByte, uShort, uInt, uLong).map { it.defaultType }
    }

    val arrayOf: IrSimpleFunctionSymbol get() = irBuiltIns.arrayOf
    val arrayOfNulls: IrSimpleFunctionSymbol get() = irBuiltIns.arrayOfNulls

    val array get() = irBuiltIns.arrayClass

    val byteArray get() = irBuiltIns.byteArray
    val charArray get() = irBuiltIns.charArray
    val shortArray get() = irBuiltIns.shortArray
    val intArray get() = irBuiltIns.intArray
    val longArray get() = irBuiltIns.longArray
    val floatArray get() = irBuiltIns.floatArray
    val doubleArray get() = irBuiltIns.doubleArray
    val booleanArray get() = irBuiltIns.booleanArray

    val byteArrayType get() = byteArray.owner.defaultType
    val charArrayType get() = charArray.owner.defaultType
    val shortArrayType get() = shortArray.owner.defaultType
    val intArrayType get() = intArray.owner.defaultType
    val longArrayType get() = longArray.owner.defaultType
    val floatArrayType get() = floatArray.owner.defaultType
    val doubleArrayType get() = doubleArray.owner.defaultType
    val booleanArrayType get() = booleanArray.owner.defaultType

    val primitiveTypesToPrimitiveArrays get() = irBuiltIns.primitiveTypesToPrimitiveArrays
    val primitiveArraysToPrimitiveTypes get() = irBuiltIns.primitiveArraysToPrimitiveTypes
    val unsignedTypesToUnsignedArrays get() = irBuiltIns.unsignedTypesToUnsignedArrays

    val arrays get() = primitiveTypesToPrimitiveArrays.values + unsignedTypesToUnsignedArrays.values + array

    val collection get() = irBuiltIns.collectionClass
    val set get() = irBuiltIns.setClass
    val list get() = irBuiltIns.listClass
    val map get() = irBuiltIns.mapClass
    val mapEntry get() = irBuiltIns.mapEntryClass
    val iterable get() = irBuiltIns.iterableClass
    val listIterator get() = irBuiltIns.listIteratorClass
    val mutableCollection get() = irBuiltIns.mutableCollectionClass
    val mutableSet get() = irBuiltIns.mutableSetClass
    val mutableList get() = irBuiltIns.mutableListClass
    val mutableMap get() = irBuiltIns.mutableMapClass
    val mutableMapEntry get() = irBuiltIns.mutableMapEntryClass
    val mutableIterable get() = irBuiltIns.mutableIterableClass
    val mutableIterator get() = irBuiltIns.mutableIteratorClass
    val mutableListIterator get() = irBuiltIns.mutableListIteratorClass
    val comparable get() = irBuiltIns.comparableClass

    private val binaryOperatorCache = mutableMapOf<Triple<Name, IrType, IrType>, IrSimpleFunctionSymbol>()

    fun getBinaryOperator(name: Name, lhsType: IrType, rhsType: IrType): IrSimpleFunctionSymbol =
        irBuiltIns.getBinaryOperator(name, lhsType, rhsType)

    fun getUnaryOperator(name: Name, receiverType: IrType): IrSimpleFunctionSymbol = irBuiltIns.getUnaryOperator(name, receiverType)

    open fun functionN(n: Int): IrClassSymbol = irBuiltIns.functionN(n).symbol
    open fun suspendFunctionN(n: Int): IrClassSymbol = irBuiltIns.suspendFunctionN(n).symbol

    fun kproperty0(): IrClassSymbol = irBuiltIns.kProperty0Class
    fun kproperty1(): IrClassSymbol = irBuiltIns.kProperty1Class
    fun kproperty2(): IrClassSymbol = irBuiltIns.kProperty2Class

    fun kmutableproperty0(): IrClassSymbol = irBuiltIns.kMutableProperty0Class
    fun kmutableproperty1(): IrClassSymbol = irBuiltIns.kMutableProperty1Class
    fun kmutableproperty2(): IrClassSymbol = irBuiltIns.kMutableProperty2Class

    val extensionToString: IrSimpleFunctionSymbol get() = irBuiltIns.extensionToString
    val memberToString: IrSimpleFunctionSymbol get() = irBuiltIns.memberToString
    val extensionStringPlus: IrSimpleFunctionSymbol get() = irBuiltIns.extensionStringPlus
    val memberStringPlus: IrSimpleFunctionSymbol get() = irBuiltIns.memberStringPlus

    fun isStringPlus(functionSymbol: IrFunctionSymbol): Boolean {
        val plusSymbol = if (functionSymbol.owner.dispatchReceiverParameter?.type?.isString() == true)
            memberStringPlus
        else if (functionSymbol.owner.extensionReceiverParameter?.type?.isNullableString() == true)
            extensionStringPlus
        else
            return false

        return functionSymbol == plusSymbol
    }
}

// Some symbols below are used in kotlin-native, so they can't be private
@Suppress("MemberVisibilityCanBePrivate", "PropertyName")
abstract class Symbols(
    irBuiltIns: IrBuiltIns, symbolTable: ReferenceSymbolTable
) : BuiltinSymbolsBase(irBuiltIns, symbolTable) {

    abstract val throwNullPointerException: IrSimpleFunctionSymbol
    abstract val throwTypeCastException: IrSimpleFunctionSymbol

    abstract val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol

    abstract val throwKotlinNothingValueException: IrSimpleFunctionSymbol

    open val throwISE: IrSimpleFunctionSymbol
        get() = error("throwISE is not implemented")

    open val throwIAE: IrSimpleFunctionSymbol
        get() = error("throwIAE is not implemented")

    abstract val stringBuilder: IrClassSymbol

    abstract val defaultConstructorMarker: IrClassSymbol

    abstract val coroutineImpl: IrClassSymbol

    abstract val coroutineSuspendedGetter: IrSimpleFunctionSymbol

    abstract val getContinuation: IrSimpleFunctionSymbol

    abstract val continuationClass: IrClassSymbol

    abstract val coroutineContextGetter: IrSimpleFunctionSymbol

    abstract val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol

    abstract val coroutineGetContext: IrSimpleFunctionSymbol

    abstract val returnIfSuspended: IrSimpleFunctionSymbol

    abstract val functionAdapter: IrClassSymbol

    open val unsafeCoerceIntrinsic: IrSimpleFunctionSymbol? = null

    open val getWithoutBoundCheckName: Name? = null

    open val setWithoutBoundCheckName: Name? = null

    open val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol>? = null

    companion object {
        fun isLateinitIsInitializedPropertyGetter(symbol: IrFunctionSymbol): Boolean =
            symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                function.name.asString() == "<get-isInitialized>" &&
                        function.isTopLevel &&
                        function.getPackageFragment().packageFqName.asString() == "kotlin" &&
                        function.valueParameters.isEmpty() &&
                        symbol.owner.extensionReceiverParameter?.type?.classOrNull?.owner.let { receiverClass ->
                            receiverClass?.fqNameWhenAvailable?.toUnsafe() == StandardNames.FqNames.kProperty0
                        }
            }

        fun isTypeOfIntrinsic(symbol: IrFunctionSymbol): Boolean =
            symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                function.name.asString() == "typeOf" &&
                        function.valueParameters.isEmpty() &&
                        (function.parent as? IrPackageFragment)?.packageFqName == KOTLIN_REFLECT_FQ_NAME
            }
    }
}
