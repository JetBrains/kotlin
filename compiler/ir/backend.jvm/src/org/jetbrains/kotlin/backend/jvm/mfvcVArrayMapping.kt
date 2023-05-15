/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.config.JvmMfvcVArrayFlatteningScheme
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

/**
 * Mapping of [LeafMfvcNode] of VArray<Mfvc> element
 *
 * Contains information how [LeafMfvcNode] of VArray<Mfvc> maps onto element of flattened array representation
 *
 * @property wrapperField Field of wrapper of flattened representation which contains the leaf value
 * @property indexInWrapperArray Index in array stored in [wrapperField] which contains the leaf value
 * @property wrapperArrayGetFunction Array *get* function for array stored in [wrapperField]
 * @property wrapperArraySetFunction Array *set* function for array stored in [wrapperField]
 * @property encodeFunction Function that returns IR for encoded value, which actually stored in [wrapperField] by original leaf value
 * @property decodeFunction Function that returns IR for decoded, original leaf value
 */
class MfvcVArrayMapping(
    val wrapperField: IrFieldSymbol,
    val indexInWrapperArray: IrExpression,
    val wrapperArrayGetFunction: IrFunctionSymbol,
    val wrapperArraySetFunction: IrFunctionSymbol,
    val encodeFunction: (IrExpression, IrBuilderWithScope) -> IrExpression,
    val decodeFunction: (IrExpression, IrBuilderWithScope) -> IrExpression
)

abstract class MfvcVArrayMapper(protected val mfvcNode: RootMfvcNode) {
    internal abstract val wrapperField: Map<LeafType, IrFieldSymbol>
    internal abstract val groupSizeForType: Map<LeafType, Int>
    internal abstract val groupSizes: List<Int>
    internal abstract val arrayConstructors: List<IrFunctionSymbol>
    internal abstract val arrayGetFunctions: Map<LeafType, IrFunctionSymbol>
    internal abstract val arraySetFunctions: Map<LeafType, IrFunctionSymbol>
    internal abstract val encodingFunctions: Map<LeafType, (IrExpression, IrBuilderWithScope) -> IrExpression>
    internal abstract val decodingFunctions: Map<LeafType, (IrExpression, IrBuilderWithScope) -> IrExpression>
    abstract val indexOfLeafInGroup: IntArray

    fun map(
        indexInVArrayVariable: IrVariable,
        leafIndex: Int,
        builder: IrBlockBuilder,
        symbols: FlatteningSymbolsHelper
    ): MfvcVArrayMapping {
        val leafType = LeafType.fromIrType(mfvcNode.leaves[leafIndex].type)

        // Suppose Mfvc has group_size leafs that stored in the same array as given leaf.
        // Suppose that given leaf is leaf_index-th in the set of leafs stored in that array.
        // Then, leaf has index indexInVArrayVariable * group_size + leaf_index in that array.
        val indexInWrapperArray = with(builder) {
            irCallOp(
                symbols.backendContext.irBuiltIns.intPlusSymbol,
                symbols.backendContext.irBuiltIns.intType,
                irCallOp(
                    symbols.backendContext.irBuiltIns.intTimesSymbol,
                    symbols.backendContext.irBuiltIns.intType,
                    irGet(indexInVArrayVariable),
                    irInt(groupSizeForType[leafType]!!)
                ),
                irInt(indexOfLeafInGroup[leafIndex])
            )
        }

        return MfvcVArrayMapping(
            wrapperField = wrapperField[leafType]!!,
            indexInWrapperArray = indexInWrapperArray,
            wrapperArrayGetFunction = arrayGetFunctions[leafType]!!,
            wrapperArraySetFunction = arraySetFunctions[leafType]!!,
            encodeFunction = encodingFunctions[leafType]!!,
            decodeFunction = decodingFunctions[leafType]!!
        )
    }

    fun getVArrayWrapperCreationCall(
        vArraySize: IrExpression,
        builder: IrBlockBuilder,
        symbols: FlatteningSymbolsHelper,
        saveVariable: (IrVariable) -> Unit
    ): IrConstructorCall =

        with(builder) {

            val vArraySizeTempVariable = savableStandaloneVariableWithSetter(
                expression = vArraySize,
                origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                saveVariable = saveVariable
            )

            fun getArrayArgument(leafsOfThisTypeCount: Int, arrayCreationFunction: IrFunctionSymbol): IrExpression {
                if (leafsOfThisTypeCount == 0) return irNull()
                val arrayType = if (arrayCreationFunction == symbols.backendContext.irBuiltIns.arrayOfNulls)
                    symbols.backendContext.irBuiltIns.arrayClass.typeWith(listOf(symbols.backendContext.irBuiltIns.anyNType))
                else
                    arrayCreationFunction.owner.returnType
                return irCall(arrayCreationFunction, arrayType).apply {
                    putValueArgument(
                        0, irCallOp(
                            context.irBuiltIns.intTimesSymbol,
                            context.irBuiltIns.intType,
                            irGet(vArraySizeTempVariable),
                            irInt(leafsOfThisTypeCount)
                        )
                    )
                    if (arrayCreationFunction == symbols.backendContext.irBuiltIns.arrayOfNulls) {
                        putTypeArgument(0, symbols.backendContext.irBuiltIns.anyNType)
                    }
                }
            }

            val wrapperConstructorArguments = buildList {
                groupSizes.zip(arrayConstructors).map { (groupSize, arrayConstructor) ->
                    add(getArrayArgument(groupSize, arrayConstructor))
                }
                add(irGet(vArraySizeTempVariable))
            }

            return irCall(symbols.vArrayWrapperConstructor).apply {
                wrapperConstructorArguments.forEachIndexed { index, argument ->
                    putValueArgument(index, argument)
                }
            }
        }
}

private object Names {
    const val GET = "get"
    const val SET = "set"
    const val ONES = "ones"
    const val TWOS = "twos"
    const val FOURS = "fours"
    const val EIGHTS = "eights"
    const val REFS = "refs"
    const val LONGS = "longs"
}

class FlatteningSymbolsHelper(val backendContext: JvmBackendContext) {

    val bytesGetFunction = backendContext.irBuiltIns.byteArray.functionByName(Names.GET)
    val shortsGetFunction = backendContext.irBuiltIns.shortArray.functionByName(Names.GET)
    val intsGetFunction = backendContext.irBuiltIns.intArray.functionByName(Names.GET)
    val longsGetFunction = backendContext.irBuiltIns.longArray.functionByName(Names.GET)
    val refsGetFunction = backendContext.irBuiltIns.arrayClass.functionByName(Names.GET)

    val bytesSetFunction = backendContext.irBuiltIns.byteArray.functionByName(Names.SET)
    val shortsSetFunction = backendContext.irBuiltIns.shortArray.functionByName(Names.SET)
    val intsSetFunction = backendContext.irBuiltIns.intArray.functionByName(Names.SET)
    val longsSetFunction = backendContext.irBuiltIns.longArray.functionByName(Names.SET)
    val refsSetFunction = backendContext.irBuiltIns.arrayClass.functionByName(Names.SET)

    val byteToLong = backendContext.irBuiltIns.byteClass.functionByName("toLong")
    val shortToLong = backendContext.irBuiltIns.shortClass.functionByName("toLong")
    val intToLong = backendContext.irBuiltIns.intClass.functionByName("toLong")

    val intToShort = backendContext.irBuiltIns.intClass.functionByName("toShort")
    val intToChar = backendContext.irBuiltIns.intClass.functionByName("toChar")
    val shortToInt = backendContext.irBuiltIns.shortClass.functionByName("toInt")

    val longToByte = backendContext.irBuiltIns.longClass.functionByName("toByte")
    val longToShort = backendContext.irBuiltIns.longClass.functionByName("toShort")
    val longToInt = backendContext.irBuiltIns.longClass.functionByName("toInt")

    val floatCompanion = backendContext.irBuiltIns.floatClass.owner.companionObject()!!.symbol
    val doubleCompanion = backendContext.irBuiltIns.doubleClass.owner.companionObject()!!.symbol

    val byteArrayConstructor = backendContext.irBuiltIns.byteArray.constructors.single { it.owner.valueParameters.size == 1 }
    val shortArrayConstructor = backendContext.irBuiltIns.shortArray.constructors.single { it.owner.valueParameters.size == 1 }
    val intArrayConstructor = backendContext.irBuiltIns.intArray.constructors.single { it.owner.valueParameters.size == 1 }
    val longArrayConstructor = backendContext.irBuiltIns.longArray.constructors.single { it.owner.valueParameters.size == 1 }

    val vArrayWrapperClass = when (backendContext.state.mfvcVArrayFlatteningScheme) {
        JvmMfvcVArrayFlatteningScheme.PER_TYPE -> TODO("Flattening scheme is not implemented yet")
        JvmMfvcVArrayFlatteningScheme.PER_SIZE -> backendContext.ir.symbols.vArrayWrapperPerSizeClass
        JvmMfvcVArrayFlatteningScheme.THREE_ARRAYS -> TODO("Flattening scheme is not implemented yet")
        JvmMfvcVArrayFlatteningScheme.TWO_ARRAYS -> backendContext.ir.symbols.vArrayWrapperTwoArrays
    }

    val vArrayWrapperIteratorStateHolderClass = when (backendContext.state.mfvcVArrayFlatteningScheme) {
        JvmMfvcVArrayFlatteningScheme.PER_TYPE -> TODO("Flattening scheme is not implemented yet")
        JvmMfvcVArrayFlatteningScheme.PER_SIZE -> backendContext.ir.symbols.vArrayPerSizeIteratorStateHolder
        JvmMfvcVArrayFlatteningScheme.THREE_ARRAYS -> TODO("Flattening scheme is not implemented yet")
        JvmMfvcVArrayFlatteningScheme.TWO_ARRAYS -> backendContext.ir.symbols.vArrayTwoArraysIteratorStateHolder
    }

    val vArrayWrapperConstructor = vArrayWrapperClass.constructors.single { it.owner.isPrimary }
    val vArrayWrapperIteratorStateHolderConstructor = vArrayWrapperIteratorStateHolderClass.constructors.single { it.owner.isPrimary }

    val vArrayGet = backendContext.irBuiltIns.vArrayClass?.functions?.single { it.owner.name == Name.identifier("get") }
    val vArraySet = backendContext.irBuiltIns.vArrayClass?.functions?.single { it.owner.name == Name.identifier("set") }
    val vArraySizeGetter = backendContext.irBuiltIns.vArrayClass?.getPropertyGetter("size")

    val wrapperSizeField = vArrayWrapperClass.fieldByName("size")
    val vArrayWrapperIteratorStateHolderIndexField = vArrayWrapperIteratorStateHolderClass.fieldByName("index")
    val vArrayWrapperIteratorStateHolderArrayField = vArrayWrapperIteratorStateHolderClass.fieldByName("array")

    val noSuchElementExceptionConstructor = backendContext.ir.symbols.noSuchElementException.constructors.single()

    val throwableMessageGetter = backendContext.irBuiltIns.throwableClass.getPropertyGetter("message")!!
}

internal enum class LeafType {
    BOOL, BYTE, SHORT, CHAR, INT, FLOAT, LONG, DOUBLE, REF;

    companion object {
        fun fromIrType(type: IrType) = when {
            type.isBoolean() -> BOOL
            type.isByte() -> BYTE
            type.isShort() -> SHORT
            type.isChar() -> CHAR
            type.isInt() -> INT
            type.isFloat() -> FLOAT
            type.isLong() -> LONG
            type.isDouble() -> DOUBLE
            else -> REF
        }
    }
}

fun boolToByte(boolExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irIfThenElse(
        type = symbols.backendContext.irBuiltIns.byteType,
        condition = boolExpr,
        thenPart = irByte(1),
        elsePart = irByte(0)
    )
}

fun boolToLong(boolExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irIfThenElse(
        type = symbols.backendContext.irBuiltIns.longType,
        condition = boolExpr,
        thenPart = irLong(1),
        elsePart = irLong(0)
    )
}

fun byteToLong(byteExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.byteToLong).apply {
        dispatchReceiver = byteExpr
    }
}

fun shortToLong(shortExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.shortToLong).apply {
        dispatchReceiver = shortExpr
    }
}

fun charToLong(charExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) =
    shortToLong(charToShort(charExpr, irBuilder, symbols), irBuilder, symbols)

fun floatToLong(floatExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) =
    intToLong(floatToInt(floatExpr, irBuilder, symbols), irBuilder, symbols)

fun intToLong(intExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.intToLong).apply {
        dispatchReceiver = intExpr
    }
}

fun numericToBool(numericExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper, zero: IrExpression) =
    with(irBuilder) {
        irCallOp(
            callee = symbols.backendContext.irBuiltIns.booleanNotSymbol,
            type = symbols.backendContext.irBuiltIns.booleanType,
            dispatchReceiver = irCall(symbols.backendContext.irBuiltIns.eqeqSymbol).apply {
                putValueArgument(0, numericExpr)
                putValueArgument(1, zero)
            }
        )
    }

fun byteToBool(byteExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) =
    numericToBool(byteExpr, irBuilder, symbols, irBuilder.irByte(0))

fun longToBool(longExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) =
    numericToBool(longExpr, irBuilder, symbols, irBuilder.irLong(0))

fun longToByte(longExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.longToByte).apply {
        dispatchReceiver = longExpr
    }
}

fun longToShort(longExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.longToShort).apply {
        dispatchReceiver = longExpr
    }
}

fun longToInt(longExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.longToInt).apply {
        dispatchReceiver = longExpr
    }
}

fun longToChar(longExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.intToChar).apply {
        dispatchReceiver = longToInt(longExpr, irBuilder, symbols)
    }
}

fun longToFloat(longExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) =
    intToFloat(longToInt(longExpr, irBuilder, symbols), irBuilder, symbols)

fun charToShort(charExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.intToShort).apply {
        dispatchReceiver = irCall(symbols.backendContext.ir.symbols.charCodeGetter).apply {
            extensionReceiver = charExpr
        }
    }
}

fun shortToChar(shortExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.intToChar).apply {
        dispatchReceiver = irCall(symbols.shortToInt).apply {
            dispatchReceiver = shortExpr
        }
    }
}


private fun floatToInt(floatExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.backendContext.ir.symbols.floatToRawBits).apply {
        extensionReceiver = floatExpr
    }
}

private fun intToFloat(intExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.backendContext.ir.symbols.floatFromBits).apply {
        extensionReceiver = irGetObject(symbols.floatCompanion)
        putValueArgument(0, intExpr)
    }
}

private fun longToDouble(longExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.backendContext.ir.symbols.doubleFromBits).apply {
        extensionReceiver = irGetObject(symbols.doubleCompanion)
        putValueArgument(0, longExpr)
    }
}

private fun doubleToLong(doubleExpr: IrExpression, irBuilder: IrBuilderWithScope, symbols: FlatteningSymbolsHelper) = with(irBuilder) {
    irCall(symbols.backendContext.ir.symbols.doubleToRawBits).apply {
        extensionReceiver = doubleExpr
    }
}

class PerSizeMapper(mvfcNode: RootMfvcNode, symbols: FlatteningSymbolsHelper) : MfvcVArrayMapper(mvfcNode) {

    private val onesField = symbols.backendContext.ir.symbols.vArrayWrapperPerSizeClass.fieldByName(Names.ONES)
    private val twosField = symbols.backendContext.ir.symbols.vArrayWrapperPerSizeClass.fieldByName(Names.TWOS)
    private val fourField = symbols.backendContext.ir.symbols.vArrayWrapperPerSizeClass.fieldByName(Names.FOURS)
    private val eightsField = symbols.backendContext.ir.symbols.vArrayWrapperPerSizeClass.fieldByName(Names.EIGHTS)
    private val refsField = symbols.backendContext.ir.symbols.vArrayWrapperPerSizeClass.fieldByName(Names.REFS)

    override val indexOfLeafInGroup: IntArray
    override val wrapperField: Map<LeafType, IrFieldSymbol>
    override val groupSizeForType: Map<LeafType, Int>
    override val groupSizes: List<Int>
    override val arrayConstructors: List<IrFunctionSymbol>
    override val arrayGetFunctions: Map<LeafType, IrFunctionSymbol>
    override val arraySetFunctions: Map<LeafType, IrFunctionSymbol>
    override val encodingFunctions: Map<LeafType, (IrExpression, IrBuilderWithScope) -> IrExpression>
    override val decodingFunctions: Map<LeafType, (IrExpression, IrBuilderWithScope) -> IrExpression>

    init {
        var onesCount = 0;
        var twosCount = 0;
        var foursCount = 0;
        var eightsCount = 0;
        var refsCount = 0;
        indexOfLeafInGroup = IntArray(mfvcNode.leavesCount)
        mfvcNode.leaves.forEachIndexed { leafIndex, leaf ->
            indexOfLeafInGroup[leafIndex] = when {
                leaf.type.isBoolean() -> onesCount++
                leaf.type.isByte() -> onesCount++
                leaf.type.isChar() -> twosCount++
                leaf.type.isShort() -> twosCount++
                leaf.type.isInt() -> foursCount++
                leaf.type.isFloat() -> foursCount++
                leaf.type.isLong() -> eightsCount++
                leaf.type.isDouble() -> eightsCount++
                else -> refsCount++
            }
        }

        groupSizes = listOf(onesCount, twosCount, foursCount, eightsCount, refsCount)

        with(symbols) {
            arrayConstructors = listOf(
                byteArrayConstructor,
                shortArrayConstructor,
                intArrayConstructor,
                longArrayConstructor,
                backendContext.irBuiltIns.arrayOfNulls
            )
        }

        wrapperField = mapOf(
            LeafType.BOOL to onesField,
            LeafType.BYTE to onesField,
            LeafType.SHORT to twosField,
            LeafType.CHAR to twosField,
            LeafType.INT to fourField,
            LeafType.FLOAT to fourField,
            LeafType.LONG to eightsField,
            LeafType.DOUBLE to eightsField,
            LeafType.REF to refsField
        )

        groupSizeForType = mapOf(
            LeafType.BOOL to onesCount,
            LeafType.BYTE to onesCount,
            LeafType.CHAR to twosCount,
            LeafType.SHORT to twosCount,
            LeafType.INT to foursCount,
            LeafType.FLOAT to foursCount,
            LeafType.LONG to eightsCount,
            LeafType.DOUBLE to eightsCount,
            LeafType.REF to refsCount
        )

        with(symbols) {
            arraySetFunctions = mapOf(
                LeafType.BOOL to bytesSetFunction,
                LeafType.BYTE to bytesSetFunction,
                LeafType.CHAR to shortsSetFunction,
                LeafType.SHORT to shortsSetFunction,
                LeafType.INT to intsSetFunction,
                LeafType.FLOAT to intsSetFunction,
                LeafType.LONG to longsSetFunction,
                LeafType.DOUBLE to longsSetFunction,
                LeafType.REF to refsSetFunction
            )
        }

        with(symbols) {
            arrayGetFunctions = mapOf(
                LeafType.BOOL to bytesGetFunction,
                LeafType.BYTE to bytesGetFunction,
                LeafType.CHAR to shortsGetFunction,
                LeafType.SHORT to shortsGetFunction,
                LeafType.INT to intsGetFunction,
                LeafType.FLOAT to intsGetFunction,
                LeafType.LONG to longsGetFunction,
                LeafType.DOUBLE to longsGetFunction,
                LeafType.REF to refsGetFunction
            )
        }

        encodingFunctions = mapOf(
            LeafType.BOOL to { boolExpr, irBuilder -> boolToByte(boolExpr, irBuilder, symbols) },
            LeafType.BYTE to { byteExpr, _ -> byteExpr },
            LeafType.SHORT to { shortExpr, _ -> shortExpr },
            LeafType.CHAR to { charExpr, irBuilder -> charToShort(charExpr, irBuilder, symbols) },
            LeafType.INT to { intExpr, _ -> intExpr },
            LeafType.FLOAT to { floatExpr, irBuilder -> floatToInt(floatExpr, irBuilder, symbols) },
            LeafType.LONG to { longExpr, _ -> longExpr },
            LeafType.DOUBLE to { doubleExpr, irBuilder -> doubleToLong(doubleExpr, irBuilder, symbols) },
            LeafType.REF to { refExpr, _ -> refExpr }
        )

        decodingFunctions = mapOf(
            LeafType.BOOL to { byteExpr, irBuilder -> byteToBool(byteExpr, irBuilder, symbols) },
            LeafType.BYTE to { byteExpr, _ -> byteExpr },
            LeafType.SHORT to { shortExpr, _ -> shortExpr },
            LeafType.CHAR to { shortExpr, irBuilder -> shortToChar(shortExpr, irBuilder, symbols) },
            LeafType.INT to { intExpr, _ -> intExpr },
            LeafType.FLOAT to { intExpr, irBuilder -> intToFloat(intExpr, irBuilder, symbols) },
            LeafType.LONG to { longExpr, _ -> longExpr },
            LeafType.DOUBLE to { longExpr, irBuilder -> longToDouble(longExpr, irBuilder, symbols) },
            LeafType.REF to { refExpr, _ -> refExpr }
        )
    }
}

class TwoArraysMapper(mvfcNode: RootMfvcNode, symbols: FlatteningSymbolsHelper) : MfvcVArrayMapper(mvfcNode) {

    private val longsField = symbols.backendContext.ir.symbols.vArrayWrapperTwoArrays.fieldByName(Names.LONGS)
    private val refsField = symbols.backendContext.ir.symbols.vArrayWrapperTwoArrays.fieldByName(Names.REFS)

    override val indexOfLeafInGroup: IntArray
    override val wrapperField: Map<LeafType, IrFieldSymbol>
    override val groupSizeForType: Map<LeafType, Int>
    override val groupSizes: List<Int>
    override val arrayConstructors: List<IrFunctionSymbol>
    override val arrayGetFunctions: Map<LeafType, IrFunctionSymbol>
    override val arraySetFunctions: Map<LeafType, IrFunctionSymbol>
    override val encodingFunctions: Map<LeafType, (IrExpression, IrBuilderWithScope) -> IrExpression>
    override val decodingFunctions: Map<LeafType, (IrExpression, IrBuilderWithScope) -> IrExpression>

    init {
        var longsCount = 0;
        var refsCount = 0;
        indexOfLeafInGroup = IntArray(mfvcNode.leavesCount)
        mfvcNode.leaves.forEachIndexed { leafIndex, leaf ->
            indexOfLeafInGroup[leafIndex] = if (leaf.type.isPrimitiveType()) longsCount++ else refsCount++
        }

        groupSizes = listOf(longsCount, refsCount)

        with(symbols) {
            arrayConstructors = listOf(longArrayConstructor, backendContext.irBuiltIns.arrayOfNulls)
        }

        wrapperField = LeafType.values().associateWith { if (it == LeafType.REF) refsField else longsField }

        groupSizeForType = LeafType.values().associateWith { if (it == LeafType.REF) refsCount else longsCount }

        arraySetFunctions =
            LeafType.values().associateWith { if (it == LeafType.REF) symbols.refsSetFunction else symbols.longsSetFunction }

        arrayGetFunctions =
            LeafType.values().associateWith { if (it == LeafType.REF) symbols.refsGetFunction else symbols.longsGetFunction }


        encodingFunctions = mapOf(
            LeafType.BOOL to { boolExpr, irBuilder -> boolToLong(boolExpr, irBuilder, symbols) },
            LeafType.BYTE to { byteExpr, irBuilder -> byteToLong(byteExpr, irBuilder, symbols) },
            LeafType.SHORT to { shortExpr, irBuilder -> shortToLong(shortExpr, irBuilder, symbols) },
            LeafType.CHAR to { charExpr, irBuilder -> charToLong(charExpr, irBuilder, symbols) },
            LeafType.INT to { intExpr, irBuilder -> intToLong(intExpr, irBuilder, symbols) },
            LeafType.FLOAT to { floatExpr, irBuilder -> floatToLong(floatExpr, irBuilder, symbols) },
            LeafType.LONG to { longExpr, _ -> longExpr },
            LeafType.DOUBLE to { doubleExpr, irBuilder -> doubleToLong(doubleExpr, irBuilder, symbols) },
            LeafType.REF to { refExpr, _ -> refExpr }
        )

        decodingFunctions = mapOf(
            LeafType.BOOL to { longExpr, irBuilder -> longToBool(longExpr, irBuilder, symbols) },
            LeafType.BYTE to { longExpr, irBuilder -> longToByte(longExpr, irBuilder, symbols) },
            LeafType.SHORT to { longExpr, irBuilder -> longToShort(longExpr, irBuilder, symbols) },
            LeafType.CHAR to { longExpr, irBuilder -> longToChar(longExpr, irBuilder, symbols) },
            LeafType.INT to { longExpr, irBuilder -> longToInt(longExpr, irBuilder, symbols) },
            LeafType.FLOAT to { longExpr, irBuilder -> longToFloat(longExpr, irBuilder, symbols) },
            LeafType.LONG to { longExpr, _ -> longExpr },
            LeafType.DOUBLE to { longExpr, irBuilder -> longToDouble(longExpr, irBuilder, symbols) },
            LeafType.REF to { refExpr, _ -> refExpr }
        )
    }
}