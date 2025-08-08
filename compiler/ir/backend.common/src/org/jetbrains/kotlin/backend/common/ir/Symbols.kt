/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import kotlin.getValue

// Some symbols below are used in kotlin-native, so they can't be private
@Suppress("MemberVisibilityCanBePrivate")
@OptIn(InternalSymbolFinderAPI::class)
abstract class Symbols(val irBuiltIns: IrBuiltIns) : FrontendSymbols {
    protected val symbolFinder = irBuiltIns.symbolFinder

    private fun getClass(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): IrClassSymbol =
        symbolFinder.findClass(name, *packageNameSegments)
            ?: error("Class '$name' not found in package '${packageNameSegments.joinToString(".")}'")

    val iterator = getClass(Name.identifier("Iterator"), "kotlin", "collections")

    val charSequence = getClass(Name.identifier("CharSequence"), "kotlin")
    val string = getClass(Name.identifier("String"), "kotlin")

    val primitiveIteratorsByType = PrimitiveType.entries.associate { type ->
        val iteratorClass = getClass(Name.identifier(type.typeName.asString() + "Iterator"), "kotlin", "collections")
        type to iteratorClass
    }

    val asserts = symbolFinder.findFunctions(Name.identifier("assert"), "kotlin")

    private fun progression(name: String) = getClass(Name.identifier(name), "kotlin", "ranges")
    private fun progressionOrNull(name: String) = symbolFinder.findClass(Name.identifier(name), "kotlin", "ranges")

    // The "...OrNull" variants are used for the classes below because the minimal stdlib used in tests do not include those classes.
    // It was not feasible to add them to the JS reduced runtime because all its transitive dependencies also need to be
    // added, which would include a lot of the full stdlib.
    val uByte = symbolFinder.findClass(Name.identifier("UByte"), "kotlin")
    val uShort = symbolFinder.findClass(Name.identifier("UShort"), "kotlin")
    val uInt = symbolFinder.findClass(Name.identifier("UInt"), "kotlin")
    val uLong = symbolFinder.findClass(Name.identifier("ULong"), "kotlin")
    val uIntProgression = progressionOrNull("UIntProgression")
    val uLongProgression = progressionOrNull("ULongProgression")
    val uIntRange = progressionOrNull("UIntRange")
    val uLongRange = progressionOrNull("ULongRange")
    val sequence = symbolFinder.findClass(Name.identifier("Sequence"), "kotlin", "sequences")

    val charProgression = progression("CharProgression")
    val intProgression = progression("IntProgression")
    val longProgression = progression("LongProgression")
    val progressionClasses = listOfNotNull(charProgression, intProgression, longProgression, uIntProgression, uLongProgression)

    val charRange = progression("CharRange")
    val intRange = progression("IntRange")
    val longRange = progression("LongRange")
    val rangeClasses = listOfNotNull(charRange, intRange, longRange, uIntRange, uLongRange)

    val closedRange = progression("ClosedRange")

    abstract val getProgressionLastElementByReturnType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>

    val toUIntByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("toUInt")).functionSymbolAssociatedBy {
        it.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type?.classifierOrFail
            ?: error("Expected extension receiver for ${it.render()}")
    }

    val toULongByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("toULong")).functionSymbolAssociatedBy {
        it.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type?.classifierOrFail
            ?: error("Expected extension receiver for ${it.render()}")
    }

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
        val plusSymbol = when {
            functionSymbol.owner.hasShape(
                dispatchReceiver = true,
                regularParameters = 1,
                parameterTypes = listOf(irBuiltIns.stringType, null)
            ) -> memberStringPlus
            functionSymbol.owner.hasShape(
                extensionReceiver = true,
                regularParameters = 1,
                parameterTypes = listOf(irBuiltIns.stringType.makeNullable(), null)
            ) -> extensionStringPlus
            else -> return false
        }

        return functionSymbol == plusSymbol
    }

    protected fun ClassId.classSymbol() = symbolFinder.findClass(this) ?: error("Class $this is not found")
    protected fun CallableId.propertySymbols() = symbolFinder.findProperties(this).toList()
    protected fun CallableId.functionSymbols() = symbolFinder.findFunctions(this).toList()
    protected fun ClassId.primaryConstructorSymbol(): Lazy<IrConstructorSymbol> {
        val clazz = classSymbol()
        return lazy { (clazz.owner.primaryConstructor ?: error("Class ${this} has no primary constructor")).symbol }
    }

    protected fun ClassId.noParametersConstructorSymbol(): Lazy<IrConstructorSymbol> {
        val clazz = classSymbol()
        return lazy { (clazz.owner.constructors.singleOrNull { it.parameters.isEmpty() } ?: error("Class ${this} has no constructor without parameters")).symbol }
    }

    protected fun CallableId.functionSymbol(): IrSimpleFunctionSymbol {
        val elements = functionSymbols()
        require(elements.isNotEmpty()) { "No function $this found" }
        require(elements.size == 1) { "Several functions $this found:\n${elements.joinToString("\n")}" }
        return elements.single()
    }

    protected inline fun CallableId.functionSymbol(crossinline condition: (IrSimpleFunction) -> Boolean): Lazy<IrSimpleFunctionSymbol> {
        val unfilteredElements = functionSymbols()
        return lazy {
            val elements = unfilteredElements.filter { condition(it.owner) }
            require(elements.isNotEmpty()) { "No function $this found corresponding given condition" }
            require(elements.size == 1) { "Several functions $this found corresponding given condition:\n${elements.joinToString("\n")}" }
            elements.single()
        }
    }

    protected inline fun <K> CallableId.functionSymbolAssociatedBy(crossinline getKey: (IrSimpleFunction) -> K): Lazy<Map<K, IrSimpleFunctionSymbol>> {
        val unfilteredElements = functionSymbols()
        return lazy {
            unfilteredElements.associateBy { getKey(it.owner) }
        }
    }

    abstract val throwNullPointerException: IrSimpleFunctionSymbol
    abstract val throwTypeCastException: IrSimpleFunctionSymbol

    abstract val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol
    abstract val throwUnsupportedOperationException: IrSimpleFunctionSymbol

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

    open fun isSideEffectFree(call: IrCall): Boolean {
        return false
    }

    companion object {
        fun isLateinitIsInitializedPropertyGetter(symbol: IrFunctionSymbol): Boolean =
            symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                function.name.asString() == "<get-isInitialized>" &&
                        function.isTopLevel &&
                        function.getPackageFragment().packageFqName.asString() == "kotlin" &&
                        function.hasShape(extensionReceiver = true) &&
                        function.parameters[0].type.classOrNull?.owner?.fqNameWhenAvailable?.toUnsafe() == StandardNames.FqNames.kProperty0
            }

        fun isTypeOfIntrinsic(symbol: IrFunctionSymbol): Boolean {
            return if (symbol.isBound) {
                symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                    function.isTopLevelInPackage("typeOf", KOTLIN_REFLECT_FQ_NAME) && function.hasShape()
                }
            } else {
                symbol.hasTopLevelEqualFqName(KOTLIN_REFLECT_FQ_NAME.asString(), "typeOf")
            }
        }
    }
}

@OptIn(InternalSymbolFinderAPI::class)
abstract class KlibSymbols(irBuiltIns: IrBuiltIns) : Symbols(irBuiltIns) {
    final override val getProgressionLastElementByReturnType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by CallableId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("getProgressionLastElement")).functionSymbolAssociatedBy {
        it.returnType.classifierOrFail
    }

    class SharedVariableBoxClassInfo(val klass: IrClassSymbol) {
        val constructor by lazy { klass.constructors.single() }
        val load by lazy { klass.getPropertyGetter("element")!! }
        val store by lazy { klass.getPropertySetter("element")!! }
    }

    private fun findSharedVariableBoxClass(suffix: String): SharedVariableBoxClassInfo {
        val classId = ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("SharedVariableBox$suffix"))
        val boxClass = symbolFinder.findClass(classId)
            ?: error("Could not find class $classId")
        return SharedVariableBoxClassInfo(boxClass)
    }

    // The SharedVariableBox family of classes exists only in non-JVM stdlib variants, hence the nullability of the properties below.
    val genericSharedVariableBox: SharedVariableBoxClassInfo = findSharedVariableBoxClass("")
    val primitiveSharedVariableBoxes: Map<IrType, SharedVariableBoxClassInfo> = PrimitiveType.entries.associate {
        irBuiltIns.primitiveTypeToIrType[it]!! to findSharedVariableBoxClass(it.typeName.asString())
    }
}
