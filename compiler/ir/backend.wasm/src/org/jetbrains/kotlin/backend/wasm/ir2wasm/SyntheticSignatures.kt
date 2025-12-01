/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.WasmHeapType
import org.jetbrains.kotlin.wasm.ir.WasmImmediate

private const val syntheticFqName = "__SYNTHETIC__"

object Synthetics {
    // FUNCTIONS
    private val createStringLiteralLatin1Signature =
        IdSignature.CommonSignature(syntheticFqName, "createStringLiteralLatin1", null, 0, null)
    private val createStringLiteralUtf16Signature =
        IdSignature.CommonSignature(syntheticFqName, "createStringLiteralUtf16", null, 0, null)
    private val fieldInitializerFunctionSignature =
        IdSignature.CommonSignature(syntheticFqName, "fieldInitializerFunction", null, 0, null)
    private val tryGetAssociatedObjectSignature =
        IdSignature.CommonSignature(syntheticFqName, "tryGetAssociatedObject", null, 0, null)
    private val startUnitTestsFunctionSignature =
        IdSignature.CommonSignature(syntheticFqName, "startUnitTestsFunction", null, 0, null)
    private val masterInitFunctionSignature =
        IdSignature.CommonSignature(syntheticFqName, "masterInitFunction", null, 0, null)

    object Functions {
        val createStringLiteralLatin1 =
            WasmImmediate.FuncIdx(createStringLiteralLatin1Signature)
        val createStringLiteralUtf16 =
            WasmImmediate.FuncIdx(createStringLiteralUtf16Signature)
        val fieldInitializerFunction =
            WasmImmediate.FuncIdx(fieldInitializerFunctionSignature)
        val associatedObjectGetter =
            WasmImmediate.FuncIdx(tryGetAssociatedObjectSignature)
        val startUnitTestsFunction =
            WasmImmediate.FuncIdx(startUnitTestsFunctionSignature)
        val masterInitFunction =
            WasmImmediate.FuncIdx(masterInitFunctionSignature)
    }

    // GLOBALS
    object Globals {
        val addressesAndLengthsGlobal =
            WasmImmediate.GlobalIdx.FieldIdx(IdSignature.CommonSignature(syntheticFqName, "addressesAndLengthsGlobal", null, 0, null))
        val stringPoolGlobal =
            WasmImmediate.GlobalIdx.FieldIdx(IdSignature.CommonSignature(syntheticFqName, "stringPoolGlobal", null, 0, null))
    }

    // GC TYPES
    private val wasmAnyArrayTypeSignature =
        IdSignature.CommonSignature(syntheticFqName, "wasmAnyArrayType", null, 0, null)
    private val specialSlotITableTypeSignature =
        IdSignature.CommonSignature(syntheticFqName, "specialSlotITableType", null, 0, null)
    private val rttiTypeSignature =
        IdSignature.CommonSignature(syntheticFqName, "rttiType", null, 0, null)
    private val wasmLongArraySignature =
        IdSignature.CommonSignature(syntheticFqName, "wasmLongArray", null, 0, null)
    private val wasmLongArrayDeclarationSignature =
        IdSignature.CommonSignature(syntheticFqName, "wasmLongArrayDeclaration", null, 0, null)
    private val wasmStringArrayTypeSignature =
        IdSignature.CommonSignature(syntheticFqName, "wasmStringArrayType", null, 0, null)
    private val byteArraySignature =
        IdSignature.CommonSignature(syntheticFqName, "byteArray", null, 0, null)
    private val associatedObjectGetterWrapperSignature =
        IdSignature.CommonSignature(syntheticFqName, "associatedObjectGetterWrapper", null, 0, null)
    private val associatedObjectGetterTypeSignature =
        IdSignature.CommonSignature(syntheticFqName, "associatedObjectGetterType", null, 0, null)
    private val stringLiteralFunctionTypeSignature =
        IdSignature.CommonSignature(syntheticFqName, "stringLiteralFunctionType", null, 0, null)
    private val parameterlessNoReturnFunctionTypeSignature =
        IdSignature.CommonSignature(syntheticFqName, "parameterlessNoReturnFunctionTypeSignature", null, 0, null)
    private val jsExceptionTagFuncTypeSignature =
        IdSignature.CommonSignature(syntheticFqName, "jsExceptionTagFuncType", null, 0, null)

    object HeapTypes {
        val wasmAnyArrayType = WasmHeapType.Type.GcType(wasmAnyArrayTypeSignature)
        val specialSlotITableType = WasmHeapType.Type.GcType(specialSlotITableTypeSignature)
        val rttiType = WasmHeapType.Type.GcType(rttiTypeSignature)
        val wasmLongArray = WasmHeapType.Type.GcType(wasmLongArraySignature)
        val wasmLongArrayDeclaration = WasmHeapType.Type.GcType(wasmLongArrayDeclarationSignature)
        val wasmStringArrayType = WasmHeapType.Type.GcType(wasmStringArrayTypeSignature)
        val byteArray = WasmHeapType.Type.GcType(byteArraySignature)
        val associatedObjectGetterType = WasmHeapType.Type.FunctionType(associatedObjectGetterTypeSignature)
    }

    object GcTypes {
        val wasmAnyArrayType = WasmImmediate.TypeIdx.GcTypeIdx(wasmAnyArrayTypeSignature)
        val specialSlotITableType = WasmImmediate.TypeIdx.GcTypeIdx(specialSlotITableTypeSignature)
        val rttiType = WasmImmediate.TypeIdx.GcTypeIdx(rttiTypeSignature)
        val wasmLongArray = WasmImmediate.TypeIdx.GcTypeIdx(wasmLongArraySignature)
        val wasmLongArrayDeclaration = WasmImmediate.TypeIdx.GcTypeIdx(wasmLongArrayDeclarationSignature)
        val wasmStringArrayType = WasmImmediate.TypeIdx.GcTypeIdx(wasmStringArrayTypeSignature)
        val byteArray = WasmImmediate.TypeIdx.GcTypeIdx(byteArraySignature)
        val associatedObjectGetterWrapper = WasmImmediate.TypeIdx.GcTypeIdx(associatedObjectGetterWrapperSignature)
        val stringLiteralFunctionType = WasmImmediate.TypeIdx.FunctionTypeIdx(stringLiteralFunctionTypeSignature)
    }

    object FunctionHeapTypes {
        val stringLiteralFunctionType = WasmHeapType.Type.FunctionType(stringLiteralFunctionTypeSignature)
        val jsExceptionTagFuncType = WasmHeapType.Type.FunctionType(jsExceptionTagFuncTypeSignature)
        val parameterlessNoReturnFunctionType = WasmHeapType.Type.FunctionType(parameterlessNoReturnFunctionTypeSignature)
        val associatedObjectGetterType = WasmHeapType.Type.FunctionType(associatedObjectGetterTypeSignature)
    }

}