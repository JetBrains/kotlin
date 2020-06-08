/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

// This is what Context collects about IR.
abstract class Ir<out T : CommonBackendContext>(val context: T, val irModule: IrModuleFragment) {

    abstract val symbols: Symbols<T>

    val defaultParameterDeclarationsCache = mutableMapOf<IrFunction, IrFunction>()

    internal val localScopeWithCounterMap = LocalDeclarationsLowering.LocalScopeWithCounterMap()

    // If irType is an inline class type, return the underlying type according to the
    // unfolding rules of the current backend. Otherwise, returns null.
    open fun unfoldInlineClassType(irType: IrType): IrType? = null

    open fun shouldGenerateHandlerParameterForDefaultBodyFun() = false
}

/**
 * Symbols for builtins that are available without any context and are not specific to any backend
 */
open class BuiltinSymbolsBase(protected val irBuiltIns: IrBuiltIns, protected val builtIns: KotlinBuiltIns, private val symbolTable: ReferenceSymbolTable) {
    protected fun builtInsPackage(vararg packageNameSegments: String) =
        builtIns.builtInsModule.getPackage(FqName.fromSegments(listOf(*packageNameSegments))).memberScope

    // consider making this public so it can be used to easily locate stdlib functions from any place (in particular, plugins and lowerings)
    private fun getSimpleFunction(
        name: Name,
        vararg packageNameSegments: String = arrayOf("kotlin"),
        condition: (SimpleFunctionDescriptor) -> Boolean
    ): IrSimpleFunctionSymbol =
        symbolTable.referenceSimpleFunction(
            builtInsPackage(*packageNameSegments).getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                .first(condition)
        )

    private fun getClass(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): IrClassSymbol =
        getClassOrNull(name, *packageNameSegments) ?: error("Class '$name' not found in package '${packageNameSegments.joinToString(".")}'")

    private fun getClassOrNull(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): IrClassSymbol? =
        (builtInsPackage(*packageNameSegments).getContributedClassifier(
            name,
            NoLookupLocation.FROM_BACKEND
        ) as? ClassDescriptor)?.let { symbolTable.referenceClass(it) }

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

    val asserts = builtInsPackage("kotlin")
        .getContributedFunctions(Name.identifier("assert"), NoLookupLocation.FROM_BACKEND)
        .map { symbolTable.referenceFunction(it) }

    private fun progression(name: String) = getClass(Name.identifier(name), "kotlin", "ranges")
    private fun progressionOrNull(name: String) = getClassOrNull(Name.identifier(name), "kotlin", "ranges")

    // The "...OrNull" variants are used for unsigned (and progressions) because the minimal stdlib used in tests do not include those
    // those classes. It was not feasible to add them to the JS reduced runtime because all its transitive dependencies also need to be
    // added, which would include a lot of the full stdlib.
    open val uByte = getClassOrNull(Name.identifier("UByte"), "kotlin")
    open val uShort = getClassOrNull(Name.identifier("UShort"), "kotlin")
    open val uInt = getClassOrNull(Name.identifier("UInt"), "kotlin")
    open val uLong = getClassOrNull(Name.identifier("ULong"), "kotlin")
    val uIntProgression = progressionOrNull("UIntProgression")
    val uLongProgression = progressionOrNull("ULongProgression")

    val charProgression = progression("CharProgression")
    val intProgression = progression("IntProgression")
    val longProgression = progression("LongProgression")
    val progressionClasses = listOfNotNull(charProgression, intProgression, longProgression, uIntProgression, uLongProgression)

    @DescriptorBasedIr
    val progressionClassesTypes = progressionClasses.map { it.descriptor.defaultType }.toSet()

    val getProgressionLastElementByReturnType = builtInsPackage("kotlin", "internal")
        .getContributedFunctions(Name.identifier("getProgressionLastElement"), NoLookupLocation.FROM_BACKEND)
        .filter { it.containingDeclaration !is BuiltInsPackageFragment }
        .map { d ->
            val c = d.returnType?.constructor?.declarationDescriptor?.let { symbolTable.referenceClassifier(it) }
            val f = symbolTable.referenceSimpleFunction(d)
            c to f
        }.toMap()

    val toUIntByExtensionReceiver = builtInsPackage("kotlin").getContributedFunctions(
        Name.identifier("toUInt"),
        NoLookupLocation.FROM_BACKEND
    ).filter { it.containingDeclaration !is BuiltInsPackageFragment && it.extensionReceiverParameter != null }
        .map { Pair(it.extensionReceiverParameter!!.type, symbolTable.referenceSimpleFunction(it)) }.toMap()

    val toULongByExtensionReceiver = builtInsPackage("kotlin").getContributedFunctions(
        Name.identifier("toULong"),
        NoLookupLocation.FROM_BACKEND
    ).filter { it.containingDeclaration !is BuiltInsPackageFragment && it.extensionReceiverParameter != null }
        .map { Pair(it.extensionReceiverParameter!!.type, symbolTable.referenceSimpleFunction(it)) }.toMap()

    val any = symbolTable.referenceClass(builtIns.any)
    val unit = symbolTable.referenceClass(builtIns.unit)

    val char = symbolTable.referenceClass(builtIns.char)

    val byte = symbolTable.referenceClass(builtIns.byte)
    val short = symbolTable.referenceClass(builtIns.short)
    val int = symbolTable.referenceClass(builtIns.int)
    val long = symbolTable.referenceClass(builtIns.long)

    val integerClasses = listOf(byte, short, int, long)

    @DescriptorBasedIr
    val integerClassesTypes = integerClasses.map { it.descriptor.defaultType }

    val arrayOf = getSimpleFunction(Name.identifier("arrayOf")) {
        it.extensionReceiverParameter == null && it.dispatchReceiverParameter == null && it.valueParameters.size == 1 &&
                it.valueParameters[0].isVararg
    }

    val primitiveArrayOfByType = PrimitiveType.values().associate { type ->
        val function = getSimpleFunction(Name.identifier(type.name.toLowerCaseAsciiOnly() + "ArrayOf")) {
            it.extensionReceiverParameter == null && it.dispatchReceiverParameter == null && it.valueParameters.size == 1 &&
                    it.valueParameters[0].isVararg
        }
        type to function
    }

    val arrayOfNulls = getSimpleFunction(Name.identifier("arrayOfNulls")) {
        it.extensionReceiverParameter == null && it.dispatchReceiverParameter == null && it.valueParameters.size == 1 &&
                KotlinBuiltIns.isInt(it.valueParameters[0].type)
    }

    val array = symbolTable.referenceClass(builtIns.array)

    private fun primitiveArrayClass(type: PrimitiveType) =
        symbolTable.referenceClass(builtIns.getPrimitiveArrayClassDescriptor(type))

    private fun unsignedArrayClass(unsignedType: UnsignedType) =
        builtIns.builtInsModule.findClassAcrossModuleDependencies(unsignedType.arrayClassId)
            ?.let { symbolTable.referenceClass(it) }

    val byteArray = primitiveArrayClass(PrimitiveType.BYTE)
    val charArray = primitiveArrayClass(PrimitiveType.CHAR)
    val shortArray = primitiveArrayClass(PrimitiveType.SHORT)
    val intArray = primitiveArrayClass(PrimitiveType.INT)
    val longArray = primitiveArrayClass(PrimitiveType.LONG)
    val floatArray = primitiveArrayClass(PrimitiveType.FLOAT)
    val doubleArray = primitiveArrayClass(PrimitiveType.DOUBLE)
    val booleanArray = primitiveArrayClass(PrimitiveType.BOOLEAN)

    val unsignedArrays = UnsignedType.values().mapNotNull { unsignedType ->
        unsignedArrayClass(unsignedType)?.let { unsignedType to it }
    }.toMap()


    val primitiveArrays = PrimitiveType.values().associate { it to primitiveArrayClass(it) }

    val arrays = primitiveArrays.values + unsignedArrays.values + array

    val collection = symbolTable.referenceClass(builtIns.collection)
    val set = symbolTable.referenceClass(builtIns.set)
    val list = symbolTable.referenceClass(builtIns.list)
    val map = symbolTable.referenceClass(builtIns.map)
    val mapEntry = symbolTable.referenceClass(builtIns.mapEntry)
    val iterable = symbolTable.referenceClass(builtIns.iterable)
    val listIterator = symbolTable.referenceClass(builtIns.listIterator)
    val mutableCollection = symbolTable.referenceClass(builtIns.mutableCollection)
    val mutableSet = symbolTable.referenceClass(builtIns.mutableSet)
    val mutableList = symbolTable.referenceClass(builtIns.mutableList)
    val mutableMap = symbolTable.referenceClass(builtIns.mutableMap)
    val mutableMapEntry = symbolTable.referenceClass(builtIns.mutableMapEntry)
    val mutableIterable = symbolTable.referenceClass(builtIns.mutableIterable)
    val mutableIterator = symbolTable.referenceClass(builtIns.mutableIterator)
    val mutableListIterator = symbolTable.referenceClass(builtIns.mutableListIterator)

    private val binaryOperatorCache = mutableMapOf<Triple<Name, KotlinType, KotlinType>, IrSimpleFunctionSymbol>()

    fun getBinaryOperator(name: Name, lhsType: KotlinType, rhsType: KotlinType): IrSimpleFunctionSymbol {
        val key = Triple(name, lhsType, rhsType)
        return binaryOperatorCache.getOrPut(key) {
            symbolTable.referenceSimpleFunction(
                lhsType.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                    .first { it.valueParameters.size == 1 && it.valueParameters[0].type == rhsType }
            )
        }
    }

    private val unaryOperatorCache = mutableMapOf<Pair<Name, KotlinType>, IrSimpleFunctionSymbol>()

    fun getUnaryOperator(name: Name, receiverType: KotlinType): IrSimpleFunctionSymbol {
        val key = name to receiverType
        return unaryOperatorCache.getOrPut(key) {
            symbolTable.referenceSimpleFunction(
                receiverType.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                    .first { it.valueParameters.isEmpty() }
            )
        }
    }

    val intAnd = getBinaryOperator(OperatorNameConventions.AND, builtIns.intType, builtIns.intType)
    val intPlusInt = getBinaryOperator(OperatorNameConventions.PLUS, builtIns.intType, builtIns.intType)

    open fun functionN(n: Int): IrClassSymbol = irBuiltIns.function(n)
    open fun suspendFunctionN(n: Int): IrClassSymbol = irBuiltIns.suspendFunction(n)

    fun kproperty0(): IrClassSymbol = symbolTable.referenceClass(builtIns.kProperty0)
    fun kproperty1(): IrClassSymbol = symbolTable.referenceClass(builtIns.kProperty1)
    fun kproperty2(): IrClassSymbol = symbolTable.referenceClass(builtIns.kProperty2)

    fun kmutableproperty0(): IrClassSymbol = symbolTable.referenceClass(builtIns.kMutableProperty0)
    fun kmutableproperty1(): IrClassSymbol = symbolTable.referenceClass(builtIns.kMutableProperty1)
    fun kmutableproperty2(): IrClassSymbol = symbolTable.referenceClass(builtIns.kMutableProperty2)

    val extensionToString = getSimpleFunction(Name.identifier("toString")) {
        it.dispatchReceiverParameter == null && it.extensionReceiverParameter != null &&
                KotlinBuiltIns.isNullableAny(it.extensionReceiverParameter!!.type) && it.valueParameters.size == 0
    }

    val stringPlus = getSimpleFunction(Name.identifier("plus")) {
        it.dispatchReceiverParameter == null && it.extensionReceiverParameter != null &&
                KotlinBuiltIns.isStringOrNullableString(it.extensionReceiverParameter!!.type) && it.valueParameters.size == 1 &&
                KotlinBuiltIns.isNullableAny(it.valueParameters.first().type)
    }
}

// Some symbols below are used in kotlin-native, so they can't be private
@Suppress("MemberVisibilityCanBePrivate", "PropertyName")
abstract class Symbols<out T : CommonBackendContext>(val context: T, irBuiltIns: IrBuiltIns, symbolTable: SymbolTable) :
    BuiltinSymbolsBase(irBuiltIns, context.builtIns, symbolTable) {
    abstract val ThrowNullPointerException: IrFunctionSymbol
    abstract val ThrowNoWhenBranchMatchedException: IrFunctionSymbol
    abstract val ThrowTypeCastException: IrFunctionSymbol

    abstract val ThrowUninitializedPropertyAccessException: IrSimpleFunctionSymbol

    abstract val ThrowKotlinNothingValueException: IrSimpleFunctionSymbol

    abstract val stringBuilder: IrClassSymbol

    abstract val defaultConstructorMarker: IrClassSymbol

    abstract val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>

    abstract val coroutineImpl: IrClassSymbol

    abstract val coroutineSuspendedGetter: IrSimpleFunctionSymbol

    abstract val getContinuation: IrSimpleFunctionSymbol

    abstract val coroutineContextGetter: IrSimpleFunctionSymbol

    abstract val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol

    abstract val coroutineGetContext: IrSimpleFunctionSymbol

    abstract val returnIfSuspended: IrSimpleFunctionSymbol

    open val unsafeCoerceIntrinsic: IrSimpleFunctionSymbol? = null

    companion object {
        fun isLateinitIsInitializedPropertyGetter(symbol: IrFunctionSymbol): Boolean =
            symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                function.name.asString() == "<get-isInitialized>" &&
                        function.isTopLevel &&
                        function.getPackageFragment()!!.fqName.asString() == "kotlin" &&
                        function.valueParameters.isEmpty() &&
                        symbol.owner.extensionReceiverParameter?.type?.classOrNull?.owner.let { receiverClass ->
                            receiverClass?.fqNameWhenAvailable?.toUnsafe() == KotlinBuiltIns.FQ_NAMES.kProperty0
                        }
            }

        fun isTypeOfIntrinsic(symbol: IrFunctionSymbol): Boolean =
            symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                function.name.asString() == "typeOf" &&
                        function.valueParameters.isEmpty() &&
                        (function.parent as? IrPackageFragment)?.fqName == KOTLIN_REFLECT_FQ_NAME
            }
    }
}
