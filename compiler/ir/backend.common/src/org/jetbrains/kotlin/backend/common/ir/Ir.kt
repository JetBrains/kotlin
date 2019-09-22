/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.builtins.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions

// This is what Context collects about IR.
abstract class Ir<out T : CommonBackendContext>(val context: T, val irModule: IrModuleFragment) {

    abstract val symbols: Symbols<T>

    val defaultParameterDeclarationsCache = mutableMapOf<IrFunction, IrFunction>()

    // If irType is an inline class type, return the underlying type according to the
    // unfolding rules of the current backend. Otherwise, returns null.
    open fun unfoldInlineClassType(irType: IrType): IrType? = null

    open fun shouldGenerateHandlerParameterForDefaultBodyFun() = false
}

// Some symbols below are used in kotlin-native, so they can't be private
@Suppress("MemberVisibilityCanBePrivate", "PropertyName")
abstract class Symbols<out T : CommonBackendContext>(val context: T, private val symbolTable: ReferenceSymbolTable) {

    protected val builtIns
        get() = context.builtIns

    protected fun builtInsPackage(vararg packageNameSegments: String) =
        context.builtIns.builtInsModule.getPackage(FqName.fromSegments(listOf(*packageNameSegments))).memberScope

    private fun getSimpleFunction(
        name: Name,
        vararg packageNameSegments: String = arrayOf("kotlin"),
        condition: (SimpleFunctionDescriptor) -> Boolean
    ): IrSimpleFunction =
        symbolTable.referenceSimpleFunction(
            builtInsPackage(*packageNameSegments).getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                .first(condition)
        ).owner

    private fun getClass(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): IrClass =
        symbolTable.referenceClass(
            builtInsPackage(*packageNameSegments).getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor
        ).owner

    /**
     * Use this table to reference external dependencies.
     */
    open val externalSymbolTable: ReferenceSymbolTable
        get() = symbolTable

    abstract val ThrowNullPointerException: IrFunction
    abstract val ThrowNoWhenBranchMatchedException: IrFunction
    abstract val ThrowTypeCastException: IrFunction

    abstract val ThrowUninitializedPropertyAccessException: IrSimpleFunction

    abstract val stringBuilder: IrClass

    abstract val defaultConstructorMarker: IrClass

    val iterator = getClass(Name.identifier("Iterator"), "kotlin", "collections")

    val charSequence = getClass(Name.identifier("CharSequence"), "kotlin")

    val primitiveIteratorsByType = PrimitiveType.values().associate { type ->
        val iteratorClass = getClass(Name.identifier(type.typeName.asString() + "Iterator"), "kotlin", "collections")
        type to iteratorClass
    }

    val asserts = builtInsPackage("kotlin")
        .getContributedFunctions(Name.identifier("assert"), NoLookupLocation.FROM_BACKEND)
        .map { symbolTable.referenceFunction(it) }

    private fun progression(name: String) = getClass(Name.identifier(name), "kotlin", "ranges")

    val charProgression = progression("CharProgression")
    val intProgression = progression("IntProgression")
    val longProgression = progression("LongProgression")
    val progressionClasses = listOf(charProgression, intProgression, longProgression)
    val progressionClassesTypes = progressionClasses.map { it.descriptor.defaultType }.toSet()

    val any = symbolTable.referenceClass(builtIns.any)
    val unit = symbolTable.referenceClass(builtIns.unit)

    val char = symbolTable.referenceClass(builtIns.char)

    val byte = symbolTable.referenceClass(builtIns.byte)
    val short = symbolTable.referenceClass(builtIns.short)
    val int = symbolTable.referenceClass(builtIns.int)
    val long = symbolTable.referenceClass(builtIns.long)

    val integerClasses = listOf(byte, short, int, long)
    val integerClassesTypes = integerClasses.map { it.descriptor.defaultType }

    val arrayOf = getSimpleFunction(Name.identifier("arrayOf")) {
        it.extensionReceiverParameter == null && it.dispatchReceiverParameter == null && it.valueParameters.size == 1 &&
                it.valueParameters[0].isVararg
    }

    val primitiveArrayOfByType = PrimitiveType.values().associate { type ->
        val function = getSimpleFunction(Name.identifier(type.name.toLowerCase() + "ArrayOf")) {
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
        symbolTable.referenceClass(builtIns.getPrimitiveArrayClassDescriptor(type)).owner

    private fun unsignedArrayClass(unsignedType: UnsignedType) =
        builtIns.builtInsModule.findClassAcrossModuleDependencies(unsignedType.arrayClassId)
            ?.let { symbolTable.referenceClass(it).owner }

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

    abstract val copyRangeTo: Map<ClassDescriptor, IrSimpleFunction>

    abstract val coroutineImpl: IrClass

    abstract val coroutineSuspendedGetter: IrSimpleFunction

    abstract val getContinuation: IrSimpleFunction

    abstract val coroutineContextGetter: IrSimpleFunction

    abstract val suspendCoroutineUninterceptedOrReturn: IrSimpleFunction

    abstract val coroutineGetContext: IrSimpleFunction

    abstract val returnIfSuspended: IrSimpleFunction

    private val binaryOperatorCache = mutableMapOf<Triple<Name, KotlinType, KotlinType>, IrSimpleFunction>()

    fun getBinaryOperator(name: Name, lhsType: KotlinType, rhsType: KotlinType): IrSimpleFunction {
        val key = Triple(name, lhsType, rhsType)
        return binaryOperatorCache.getOrPut(key) {
            symbolTable.referenceSimpleFunction(
                lhsType.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                    .first { it.valueParameters.size == 1 && it.valueParameters[0].type == rhsType }
            ).owner
        }
    }

    private val unaryOperatorCache = mutableMapOf<Pair<Name, KotlinType>, IrSimpleFunction>()

    fun getUnaryOperator(name: Name, receiverType: KotlinType): IrSimpleFunction {
        val key = name to receiverType
        return unaryOperatorCache.getOrPut(key) {
            symbolTable.referenceSimpleFunction(
                receiverType.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                    .first { it.valueParameters.isEmpty() }
            ).owner
        }
    }

    val intAnd = getBinaryOperator(OperatorNameConventions.AND, builtIns.intType, builtIns.intType)
    val intPlusInt = getBinaryOperator(OperatorNameConventions.PLUS, builtIns.intType, builtIns.intType)

    open fun functionN(n: Int): IrClass = symbolTable.referenceClass(builtIns.getFunction(n))
    open fun suspendFunctionN(n: Int): IrClass = symbolTable.referenceClass(builtIns.getSuspendFunction(n))

    val extensionToString = getSimpleFunction(Name.identifier("toString")) {
        it.dispatchReceiverParameter == null && it.extensionReceiverParameter != null &&
                KotlinBuiltIns.isNullableAny(it.extensionReceiverParameter!!.type) && it.valueParameters.size == 0
    }

    val stringPlus = getSimpleFunction(Name.identifier("plus")) {
        it.dispatchReceiverParameter == null && it.extensionReceiverParameter != null &&
                KotlinBuiltIns.isStringOrNullableString(it.extensionReceiverParameter!!.type) && it.valueParameters.size == 1 &&
                KotlinBuiltIns.isNullableAny(it.valueParameters.first().type)
    }

    companion object {
        fun isLateinitIsInitializedPropertyGetter(function: IrFunction): Boolean =
            function is IrSimpleFunction &&
                    function.name.asString() == "<get-isInitialized>" &&
                    function.isTopLevel &&
                    function.getPackageFragment()!!.fqName.asString() == "kotlin" &&
                    function.valueParameters.isEmpty() &&
                    function.extensionReceiverParameter?.type?.classOrNull?.owner.let { receiverClass ->
                        receiverClass?.fqNameWhenAvailable?.toUnsafe() == KotlinBuiltIns.FQ_NAMES.kProperty0
                    }

        fun isTypeOfIntrinsic(function: IrFunction): Boolean =
            function is IrSimpleFunction &&
                    function.name.asString() == "typeOf" &&
                    function.valueParameters.isEmpty() &&
                    (function.parent as? IrPackageFragment)?.fqName == KOTLIN_REFLECT_FQ_NAME
    }
}
