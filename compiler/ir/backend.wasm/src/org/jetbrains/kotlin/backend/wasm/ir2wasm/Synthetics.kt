/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.util.IdSignature

private const val syntheticFqName = "__SYNTHETIC__"

private fun String.toSyntheticSignature() =
    IdSignature.CommonSignature(syntheticFqName, this, null, 0, null)

object Synthetics {
    // FUNCTIONS
    object Functions {
        val createStringLiteralLatin1 = FuncSymbol("createStringLiteralLatin1".toSyntheticSignature())
        val createStringLiteralUtf16 = FuncSymbol("createStringLiteralUtf16".toSyntheticSignature())
        val createStringLiteralJsString = FuncSymbol("createStringLiteralJsString".toSyntheticSignature())
        val fieldInitializerFunction = FuncSymbol("fieldInitializerFunction".toSyntheticSignature())
        val associatedObjectGetter = FuncSymbol("tryGetAssociatedObject".toSyntheticSignature())
        val startUnitTestsFunction = FuncSymbol("startUnitTestsFunction".toSyntheticSignature())
        val masterInitFunction = FuncSymbol("masterInitFunction".toSyntheticSignature())

        val createStringBuiltIn = FuncSymbol("createStringBuiltInFunction".toSyntheticSignature())
        val tryGetAssociatedObjectBuiltIn = FuncSymbol("tryGetAssociatedObjectFunction".toSyntheticSignature())
        val jsToKotlinAnyAdapterBuiltIn = FuncSymbol("jsToKotlinAnyAdapterBuiltInFunction".toSyntheticSignature())
        val jsToKotlinStringAdapterBuiltIn = FuncSymbol("jsToKotlinStringAdapterBuiltInFunction".toSyntheticSignature())
        val unitGetInstanceBuiltIn = FuncSymbol("unitGetInstanceBuiltInFunction".toSyntheticSignature())
        val runRootSuitesBuiltIn = FuncSymbol("runRootSuitesBuiltInFunction".toSyntheticSignature())
        val registerModuleDescriptorBuiltIn = FuncSymbol("registerModuleDescriptorBuiltInInFunction".toSyntheticSignature())
    }

    // GLOBALS
    object Globals {
        val addressesAndLengthsGlobal = FieldGlobalSymbol("addressesAndLengthsGlobal".toSyntheticSignature())
        val stringPoolGlobal = FieldGlobalSymbol("stringPoolGlobal".toSyntheticSignature())
    }

    // GC TYPES
    private val wasmAnyArrayTypeSignature = "wasmAnyArrayType".toSyntheticSignature()
    private val specialSlotITableTypeSignature = "specialSlotITableType".toSyntheticSignature()
    private val rttiTypeSignature = "rttiType".toSyntheticSignature()
    private val wasmLongArraySignature = "wasmLongArray".toSyntheticSignature()
    private val wasmLongArrayDeclarationSignature = "wasmLongArrayDeclaration".toSyntheticSignature()
    private val wasmStringArrayTypeSignature = "wasmStringArrayType".toSyntheticSignature()
    private val byteArraySignature = "byteArray".toSyntheticSignature()
    private val associatedObjectGetterWrapperSignature = "associatedObjectGetterWrapper".toSyntheticSignature()
    private val associatedObjectGetterTypeSignature = "associatedObjectGetterType".toSyntheticSignature()
    private val stringLiteralFunctionTypeSignature = "stringLiteralFunctionType".toSyntheticSignature()
    private val stringLiteralJsStringFunctionTypeSignature = "stringLiteralJsStringFunctionType".toSyntheticSignature()
    private val parameterlessNoReturnFunctionTypeSignature = "parameterlessNoReturnFunctionTypeSignature".toSyntheticSignature()
    private val jsExceptionTagFuncTypeSignature = "jsExceptionTagFuncType".toSyntheticSignature()

    private val throwableBuiltInTypeSignature = "throwableBuiltInType".toSyntheticSignature()
    private val anyBuiltInTypeSignature = "anyBuiltInType".toSyntheticSignature()

    object HeapTypes {
        val wasmAnyArrayType = GcHeapTypeSymbol(wasmAnyArrayTypeSignature)
        val specialSlotITableType = GcHeapTypeSymbol(specialSlotITableTypeSignature)
        val rttiType = GcHeapTypeSymbol(rttiTypeSignature)
        val wasmLongArray = GcHeapTypeSymbol(wasmLongArraySignature)
        val wasmLongArrayDeclaration = GcHeapTypeSymbol(wasmLongArrayDeclarationSignature)
        val wasmStringArrayType = GcHeapTypeSymbol(wasmStringArrayTypeSignature)
        val byteArray = GcHeapTypeSymbol(byteArraySignature)
        val associatedObjectGetterType = FunctionHeapTypeSymbol(associatedObjectGetterTypeSignature)
        val associatedObjectGetterWrapper = GcHeapTypeSymbol(associatedObjectGetterWrapperSignature)
        val throwableBuiltInType = GcHeapTypeSymbol(throwableBuiltInTypeSignature)
        val anyBuiltInType = GcHeapTypeSymbol(anyBuiltInTypeSignature)
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
        val stringLiteralJsStringFunctionType = FunctionTypeSymbol(stringLiteralJsStringFunctionTypeSignature)
    }

    object FunctionHeapTypes {
        val stringLiteralFunctionType = FunctionHeapTypeSymbol(stringLiteralFunctionTypeSignature)
        val jsStringLiteralFunctionType = FunctionHeapTypeSymbol(stringLiteralJsStringFunctionTypeSignature)
        val jsExceptionTagFuncType = FunctionHeapTypeSymbol(jsExceptionTagFuncTypeSignature)
        val parameterlessNoReturnFunctionType = FunctionHeapTypeSymbol(parameterlessNoReturnFunctionTypeSignature)
        val associatedObjectGetterType = FunctionHeapTypeSymbol(associatedObjectGetterTypeSignature)
    }

}