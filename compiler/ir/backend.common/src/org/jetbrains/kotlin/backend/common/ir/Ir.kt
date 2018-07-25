package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
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

abstract class Symbols<out T : CommonBackendContext>(val context: T, private val symbolTable: ReferenceSymbolTable) {

    protected val builtIns
        get() = context.builtIns

    protected fun builtInsPackage(vararg packageNameSegments: String) =
        context.builtIns.builtInsModule.getPackage(FqName.fromSegments(listOf(*packageNameSegments))).memberScope


    // This hack allows to disable related symbol instantiation in descendants
    // TODO move relevant symbols to non-common code
    open fun calc(initializer: () -> IrClassSymbol): IrClassSymbol {
        return initializer()
    }

    /**
     * Use this table to reference external dependencies.
     */
    open val externalSymbolTable: ReferenceSymbolTable
        get() = symbolTable

//    val refClass = calc { symbolTable.referenceClass(context.getInternalClass("Ref")) }

    //abstract val areEqualByValue: List<IrFunctionSymbol>

    abstract val areEqual: IrFunctionSymbol

    abstract val ThrowNullPointerException: IrFunctionSymbol
    abstract val ThrowNoWhenBranchMatchedException: IrFunctionSymbol
    abstract val ThrowTypeCastException: IrFunctionSymbol

    abstract val ThrowUninitializedPropertyAccessException: IrSimpleFunctionSymbol

    abstract val stringBuilder: IrClassSymbol

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

//    val checkProgressionStep = context.getInternalFunctions("checkProgressionStep")
//            .map { Pair(it.returnType, symbolTable.referenceSimpleFunction(it)) }.toMap()
//    val getProgressionLast = context.getInternalFunctions("getProgressionLast")
//            .map { Pair(it.returnType, symbolTable.referenceSimpleFunction(it)) }.toMap()

    val defaultConstructorMarker = symbolTable.referenceClass(context.getInternalClass("DefaultConstructorMarker"))

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
            }
                ?: throw Error(type.toString())
        return symbolTable.referenceSimpleFunction(descriptor)
    }

    abstract val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>

//    val valuesForEnum = symbolTable.referenceSimpleFunction(
//            context.getInternalFunctions("valuesForEnum").single())
//
//    val valueOfForEnum = symbolTable.referenceSimpleFunction(
//            context.getInternalFunctions("valueOfForEnum").single())

//    val getContinuation = symbolTable.referenceSimpleFunction(
//            context.getInternalFunctions("getContinuation").single())

    abstract val coroutineImpl: IrClassSymbol

    abstract val coroutineSuspendedGetter: IrSimpleFunctionSymbol

    val functionReference = calc { symbolTable.referenceClass(context.getInternalClass("FunctionReference")) }

    fun getFunction(name: Name, receiverType: KotlinType, vararg argTypes: KotlinType) =
        symbolTable.referenceFunction(receiverType.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                                          .first {
                                              var i = 0
                                              it.valueParameters.size == argTypes.size && it.valueParameters.all { type -> type == argTypes[i++] }
                                          }
        )

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
}