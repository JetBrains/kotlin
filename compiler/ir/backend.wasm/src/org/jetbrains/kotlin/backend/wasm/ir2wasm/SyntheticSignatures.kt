/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.util.IdSignature

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
        val createStringLiteralLatin1 = FuncSymbol(createStringLiteralLatin1Signature)
        val createStringLiteralUtf16 = FuncSymbol(createStringLiteralUtf16Signature)
        val fieldInitializerFunction = FuncSymbol(fieldInitializerFunctionSignature)
        val associatedObjectGetter = FuncSymbol(tryGetAssociatedObjectSignature)
        val startUnitTestsFunction = FuncSymbol(startUnitTestsFunctionSignature)
        val masterInitFunction = FuncSymbol(masterInitFunctionSignature)
    }

    // GLOBALS
    object Globals {
        val addressesAndLengthsGlobal =
            FieldGlobalSymbol(IdSignature.CommonSignature(syntheticFqName, "addressesAndLengthsGlobal", null, 0, null))
        val stringPoolGlobal =
            FieldGlobalSymbol(IdSignature.CommonSignature(syntheticFqName, "stringPoolGlobal", null, 0, null))
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
        val wasmAnyArrayType = GcHeapTypeSymbol(wasmAnyArrayTypeSignature)
        val specialSlotITableType = GcHeapTypeSymbol(specialSlotITableTypeSignature)
        val rttiType = GcHeapTypeSymbol(rttiTypeSignature)
        val wasmLongArray = GcHeapTypeSymbol(wasmLongArraySignature)
        val wasmLongArrayDeclaration = GcHeapTypeSymbol(wasmLongArrayDeclarationSignature)
        val wasmStringArrayType = GcHeapTypeSymbol(wasmStringArrayTypeSignature)
        val byteArray = GcHeapTypeSymbol(byteArraySignature)
        val associatedObjectGetterType = FunctionHeapTypeSymbol(associatedObjectGetterTypeSignature)
    }

    object GcTypes {
        val wasmAnyArrayType = GcTypeSymbol(wasmAnyArrayTypeSignature)
        val specialSlotITableType = GcTypeSymbol(specialSlotITableTypeSignature)
        val rttiType = GcTypeSymbol(rttiTypeSignature)
        val wasmLongArray = GcTypeSymbol(wasmLongArraySignature)
        val wasmLongArrayDeclaration = GcTypeSymbol(wasmLongArrayDeclarationSignature)
        val wasmStringArrayType = GcTypeSymbol(wasmStringArrayTypeSignature)
        val byteArray = GcTypeSymbol(byteArraySignature)
        val associatedObjectGetterWrapper = GcTypeSymbol(associatedObjectGetterWrapperSignature)
        val stringLiteralFunctionType = FunctionTypeSymbol(stringLiteralFunctionTypeSignature)
    }

    object FunctionHeapTypes {
        val stringLiteralFunctionType = FunctionHeapTypeSymbol(stringLiteralFunctionTypeSignature)
        val jsExceptionTagFuncType = FunctionHeapTypeSymbol(jsExceptionTagFuncTypeSignature)
        val parameterlessNoReturnFunctionType = FunctionHeapTypeSymbol(parameterlessNoReturnFunctionTypeSignature)
        val associatedObjectGetterType = FunctionHeapTypeSymbol(associatedObjectGetterTypeSignature)
    }

}