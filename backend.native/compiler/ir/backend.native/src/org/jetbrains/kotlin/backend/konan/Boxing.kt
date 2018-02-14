/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.*
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.types.KotlinType

internal fun KonanSymbols.getTypeConversion(
        actualType: KotlinType,
        expectedType: KotlinType
): IrSimpleFunctionSymbol? {
    val actualValueType = actualType.correspondingValueType
    val expectedValueType = expectedType.correspondingValueType

    return when {
        actualValueType == expectedValueType -> null

        actualValueType == null && expectedValueType != null -> {
            // This may happen in the following cases:
            // 1.  `actualType` is `Nothing`;
            // 2.  `actualType` is incompatible.

            this.getUnboxFunction(expectedValueType)
        }

        actualValueType != null && expectedValueType == null -> {
            this.boxFunctions[actualValueType]!!
        }

        else -> throw IllegalArgumentException("actual type is $actualType, expected $expectedType")
    }
}

internal fun KonanSymbols.getUnboxFunction(valueType: ValueType): IrSimpleFunctionSymbol =
        this.unboxFunctions[valueType]
                ?: this.boxClasses[valueType]!!.getPropertyGetter("value")!! as IrSimpleFunctionSymbol


/**
 * Represents static array of boxes.
 */

internal enum class BoxCache(val valueType: ValueType) {
    BYTE(ValueType.BYTE),
    SHORT(ValueType.SHORT),
    CHAR(ValueType.CHAR),
    INT(ValueType.INT),
    LONG(ValueType.LONG);

    private val valueTypeName = valueType.name.toLowerCase().capitalize()
    private val getIntrinsic = "konan.internal.getCached${valueTypeName}Box"
    private val checkIntrinsic = "konan.internal.in${valueTypeName}BoxCache"

    val cacheName = "${valueTypeName}Boxes"
    val rangeStartName = "${valueTypeName}RangeStart"
    val rangeEndName = "${valueTypeName}RangeEnd"

    companion object {
        /**
         * returns cache corresponding to the given getIntrinsic name.
         */
        fun getCacheByBoxGetter(getBoxMethodName: String): BoxCache? =
            BoxCache.values().firstOrNull { it.getIntrinsic == getBoxMethodName }

        /**
         * returns cache corresponding to the given checkIntrinsic name.
         */
        fun getCacheByInRangeChecker(checkRangeMethodName: String): BoxCache? =
            BoxCache.values().firstOrNull { it.checkIntrinsic == checkRangeMethodName }

        /**
         * Initialize globals.
         */
        fun initialize(context: Context) {
            values().forEach {
                it.initRange(context)
                it.initCache(context)
            }
        }
    }
}

/**
 * Adds global that refers to the cache.
 * If output target is native binary then the cache is created.
 */
private fun BoxCache.initCache(context: Context): LLVMValueRef {
    return if (context.config.produce.isNativeBinary) {
        context.llvm.staticData.createBoxes(this)
    } else {
        context.llvm.staticData.addGlobal(cacheName, getLlvmType(context), false)
    }
}

/**
 * Creates globals that defines the smallest and the biggest cached values.
 */
private fun BoxCache.initRange(context: Context) {
    if (context.config.produce.isNativeBinary) {
        val (start, end) = getRange(context)
        // Constancy of these globals allows LLVM's constant propagation and DCE
        // to remove fast path of boxing function in case of empty range.
        context.llvm.staticData.placeGlobal(rangeStartName, createConstant(start), true)
                .setConstant(true)
        context.llvm.staticData.placeGlobal(rangeEndName, createConstant(end), true)
                .setConstant(true)
    } else {
        context.llvm.staticData.addGlobal(rangeStartName, valueType.llvmType, false)
        context.llvm.staticData.addGlobal(rangeEndName, valueType.llvmType, false)
    }
}

private fun BoxCache.llvmRange(context: Context): Pair<LLVMValueRef, LLVMValueRef> {
    val start = LLVMGetNamedGlobal(context.llvmModule, rangeStartName)!!
    val end = LLVMGetNamedGlobal(context.llvmModule, rangeEndName)!!
    return Pair(start, end)
}

/**
 * Checks that box for the given [value] is in the cache.
 */
internal fun BoxCache.inRange(codegen: FunctionGenerationContext, value: LLVMValueRef): LLVMValueRef {
    val (startPtr, endPtr) = llvmRange(codegen.context)
    val start = codegen.load(startPtr)
    val end = codegen.load(endPtr)
    val startCheck = codegen.icmpGe(value, start)
    val endCheck = codegen.icmpLe(value, end)
    return codegen.and(startCheck, endCheck)
}

private fun BoxCache.getRange(context: Context) =
        context.config.target.getBoxCacheRange(valueType)

internal fun BoxCache.getCachedValue(codegen: FunctionGenerationContext, value: LLVMValueRef): LLVMValueRef {
    val startPtr = llvmRange(codegen.context).first
    val start = codegen.load(startPtr)
    // We should subtract range start to get index of the box.
    val index = if (this == BoxCache.BYTE) {
        // ByteBox range start has type of i8 and it can't handle values
        // that are greater than 127. So we need to cast them to i32.
        val startAsInt = codegen.sext(start, LLVMInt32Type()!!)
        val valueAsInt = codegen.sext(value, LLVMInt32Type()!!)
        LLVMBuildSub(codegen.builder, valueAsInt, startAsInt, "index")!!
    } else {
        LLVMBuildSub(codegen.builder, value, start, "index")!!
    }
    val cache = LLVMGetNamedGlobal(codegen.context.llvmModule, cacheName)!!
    val elemPtr = codegen.gep(cache, index)
    return codegen.bitcast(codegen.kObjHeaderPtr, elemPtr)
}

private fun StaticData.createBoxes(box: BoxCache): LLVMValueRef {
    val kotlinType = context.ir.symbols.boxClasses[box.valueType]!!.descriptor.defaultType
    val (start, end) = box.getRange(context)
    val values = (start..end).map { createInitializer(kotlinType, box.createConstant(it)) }
    return placeGlobalConstArray(box.cacheName, box.getLlvmType(context), values, true).llvm
}

private fun BoxCache.createConstant(value: Int) =
    constValue(when (valueType) {
        ValueType.BYTE  -> LLVMConstInt(LLVMInt8Type(),  value.toByte().toLong(),  1)!!
        ValueType.CHAR  -> LLVMConstInt(LLVMInt16Type(), value.toChar().toLong(),  0)!!
        ValueType.SHORT -> LLVMConstInt(LLVMInt16Type(), value.toShort().toLong(), 1)!!
        ValueType.INT   -> LLVMConstInt(LLVMInt32Type(), value.toLong(),   1)!!
        ValueType.LONG  -> LLVMConstInt(LLVMInt64Type(), value.toLong(),             1)!!
        else            -> error("Cannot box value of type $valueType")
    })

private fun BoxCache.getLlvmType(context: Context) =
        structType(context.llvm.runtime.objHeaderType, valueType.llvmType)

private val ValueType.llvmType
    get() = when (this) {
        ValueType.BYTE  -> LLVMInt8Type()!!
        ValueType.CHAR  -> LLVMInt16Type()!!
        ValueType.SHORT -> LLVMInt16Type()!!
        ValueType.INT   -> LLVMInt32Type()!!
        ValueType.LONG  -> LLVMInt64Type()!!
        else            -> error("Cannot box value of type $this")
    }

// When start is greater than end then `inRange` check is always false
// and can be eliminated by LLVM.
private val emptyRange = 1 to 0

// Memory usage is around 20kb.
private val defaultCacheRanges = mapOf(
        ValueType.BYTE  to (-128 to 127),
        ValueType.SHORT to (-128 to 127),
        ValueType.CHAR  to (0 to 255),
        ValueType.INT   to (-128 to 127),
        ValueType.LONG  to (-128 to 127)
)

fun KonanTarget.getBoxCacheRange(valueType: ValueType): Pair<Int, Int> = when (this) {
    // Just an example.
    is KonanTarget.ZEPHYR   -> emptyRange
    else                    -> defaultCacheRanges[valueType]!!
}