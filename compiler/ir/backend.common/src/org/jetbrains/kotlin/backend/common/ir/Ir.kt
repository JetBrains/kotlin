 /*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
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

    open fun shouldGenerateHandlerParameterForDefaultBodyFun() = false
}

// Some symbols below are used in kotlin-native, so they can't be private
@Suppress("MemberVisibilityCanBePrivate", "PropertyName")
abstract class Symbols<out T : CommonBackendContext>(val context: T, private val symbolTable: ReferenceSymbolTable) {

    protected val builtIns
        get() = context.builtIns

    protected fun builtInsPackage(vararg packageNameSegments: String) =
        context.builtIns.builtInsModule.getPackage(FqName.fromSegments(listOf(*packageNameSegments))).memberScope


    /**
     * Use this table to reference external dependencies.
     */
    open val externalSymbolTable: ReferenceSymbolTable
        get() = symbolTable

    abstract val ThrowNullPointerException: IrFunctionSymbol
    abstract val ThrowNoWhenBranchMatchedException: IrFunctionSymbol
    abstract val ThrowTypeCastException: IrFunctionSymbol

    abstract val ThrowUninitializedPropertyAccessException: IrSimpleFunctionSymbol

    abstract val stringBuilder: IrClassSymbol

    abstract val defaultConstructorMarker: IrClassSymbol

    val iterator = symbolTable.referenceClass(
        builtInsPackage("kotlin", "collections").getContributedClassifier(
            Name.identifier("Iterator"), NoLookupLocation.FROM_BACKEND
        ) as ClassDescriptor
    )

    val asserts = builtInsPackage("kotlin")
        .getContributedFunctions(Name.identifier("assert"), NoLookupLocation.FROM_BACKEND)
        .map { symbolTable.referenceFunction(it) }

    private fun progression(name: String) = symbolTable.referenceClass(
        builtInsPackage("kotlin", "ranges").getContributedClassifier(
            Name.identifier(name), NoLookupLocation.FROM_BACKEND
        ) as ClassDescriptor
    )

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

    val arrayOf = symbolTable.referenceSimpleFunction(
        builtInsPackage("kotlin").getContributedFunctions(
            Name.identifier("arrayOf"), NoLookupLocation.FROM_BACKEND
        ).first {
            it.extensionReceiverParameter == null && it.dispatchReceiverParameter == null && it.valueParameters.size == 1 &&
                    it.valueParameters[0].isVararg
        }
    )

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

    protected fun arrayExtensionFun(type: KotlinType, name: String): IrSimpleFunctionSymbol {
        val descriptor = builtInsPackage("kotlin")
            .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
            .firstOrNull {
                it.valueParameters.isEmpty()
                        && (it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor as? ClassDescriptor)?.defaultType == type
            } ?: throw Error(type.toString())
        return symbolTable.referenceSimpleFunction(descriptor)
    }

    abstract val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>

    abstract val coroutineImpl: IrClassSymbol

    abstract val coroutineSuspendedGetter: IrSimpleFunctionSymbol

    private val binaryOperatorCache = mutableMapOf<Triple<Name, KotlinType, KotlinType>, IrFunctionSymbol>()

    fun getBinaryOperator(name: Name, lhsType: KotlinType, rhsType: KotlinType): IrFunctionSymbol {
        val key = Triple(name, lhsType, rhsType)
        var result = binaryOperatorCache[key]
        if (result == null) {
            result = symbolTable.referenceFunction(lhsType.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                                                       .first { it.valueParameters.size == 1 && it.valueParameters[0].type == rhsType }
            )
            binaryOperatorCache[key] = result
        }
        return result
    }

    private val unaryOperatorCache = mutableMapOf<Pair<Name, KotlinType>, IrFunctionSymbol>()

    fun getUnaryOperator(name: Name, receiverType: KotlinType): IrFunctionSymbol {
        val key = name to receiverType
        var result = unaryOperatorCache[key]
        if (result == null) {
            result = symbolTable.referenceFunction(receiverType.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                                                       .first { it.valueParameters.isEmpty() }
            )
            unaryOperatorCache[key] = result
        }
        return result
    }

    val intAnd = getBinaryOperator(OperatorNameConventions.AND, builtIns.intType, builtIns.intType)
    val intPlusInt = getBinaryOperator(OperatorNameConventions.PLUS, builtIns.intType, builtIns.intType)

    companion object {
        fun isLateinitIsInitializedPropertyGetter(symbol: IrFunctionSymbol): Boolean =
            symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                function.name.asString() == "<get-isInitialized>" &&
                        function.parent is IrPackageFragment &&
                        function.getPackageFragment()!!.fqName.asString() == "kotlin" &&
                        function.valueParameters.isEmpty() &&
                        (symbol.owner.extensionReceiverParameter?.type?.classifierOrNull?.owner as? IrClass).let { receiverClass ->
                            receiverClass?.fqName?.toUnsafe() == KotlinBuiltIns.FQ_NAMES.kProperty0
                        }
            }
    }
}
