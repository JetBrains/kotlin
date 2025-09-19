/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.isJsShareable
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.ir.*

class WasmTypeTransformer(
    val backendContext: WasmBackendContext,
    val wasmFileCodegenContext: WasmFileCodegenContext,
) {
    private val builtIns: IrBuiltIns = backendContext.irBuiltIns
    private val symbols = backendContext.wasmSymbols
    private val useSharedObjects = backendContext.configuration.getBoolean(WasmConfigurationKeys.WASM_USE_SHARED_OBJECTS)

    fun IrType.toWasmResultType(): WasmType? =
        when (this) {
            builtIns.unitType,
            builtIns.nothingType ->
                null

            else ->
                toWasmValueType()
        }

    fun IrType.toWasmBlockResultType(): WasmType? =
        when (this) {
            // TODO: Lower blocks with Nothing type?
            builtIns.nothingType ->
                WasmUnreachableType

            symbols.voidType ->
                null

            else ->
                toWasmValueType()
        }

    private fun IrType.toWasmGcRefType(): WasmType =
        WasmRefNullType(WasmHeapType.Type(wasmFileCodegenContext.referenceGcType(getRuntimeClass(backendContext.irBuiltIns).symbol)))

    fun IrType.toBoxedInlineClassType(): WasmType =
        toWasmGcRefType()

    fun IrType.toWasmFieldType(hasManagedExternrefAnnotation: Boolean): WasmType {
        return when (this) {
            builtIns.booleanType,
            builtIns.byteType ->
                WasmI8

            builtIns.shortType,
            builtIns.charType ->
                WasmI16

            else -> toWasmValueType(true, hasManagedExternrefAnnotation)
        }
    }

    private val irBuiltInToWasmType: HashMap<IrType, WasmType> = hashMapOf(
        builtIns.booleanType to WasmI32,
        builtIns.byteType to WasmI32,
        builtIns.shortType to WasmI32,
        builtIns.intType to WasmI32,
        builtIns.charType to WasmI32,

        builtIns.longType to WasmI64,

        builtIns.floatType to WasmF32,

        builtIns.doubleType to WasmF64,

        builtIns.nothingNType to WasmRefNullrefType.maybeShared(useSharedObjects),

        builtIns.nothingType to WasmAnyRef.maybeShared(useSharedObjects), // Value will not be created. Just using a random Wasm type.
    )

    fun IrType.toWasmValueType(isFieldType: Boolean = false, isManagedExternrefField: Boolean = false): WasmType {
        irBuiltInToWasmType[this]?.let { return it }

        if (this == symbols.voidType) {
            error("Void type can't be used as a value")
        }

        val klass = this.erasedUpperBound
        return if (klass.isExternal) {
            if (useSharedObjects && isFieldType) {
                if (!isManagedExternrefField)
                    error("Fields of external types of shared objects must be marked with @ManagedExternref")
                return WasmI64
            }

            if (useSharedObjects && klass.isJsShareable(symbols)) {
                return WasmRefNullType(WasmHeapType.SharedSimple.EXTERN)
            }

            WasmExternRef
        } else if (isBuiltInWasmRefType(this)) {
            maybeShared(
                when (val name = klass.name.identifier) {
                    "anyref" -> WasmAnyRef
                    "eqref" -> WasmEqRef
                    "structref" ->
                        WasmRefNullType(WasmHeapType.Simple.Struct)
                    "i31ref" -> WasmI31Ref
                    "SmartShareableFuncRef" if useSharedObjects -> WasmI32
                    "funcref", "SmartShareableFuncRef" -> WasmRefNullType(WasmHeapType.Simple.Func)
                    else -> error("Unknown reference type $name")
                }
            )
        } else {
            val ic = backendContext.inlineClassesUtils.getInlinedClass(this)
            if (ic != null) {
                backendContext.inlineClassesUtils.getInlineClassUnderlyingType(ic).toWasmValueType()
            } else {
                this.toWasmGcRefType()
            }
        }
    }

    private fun maybeShared(type: WasmType) = if (type is WasmReferenceType) type.maybeShared(useSharedObjects) else type
}

private val internalReftypesFqName: FqName = FqName("kotlin.wasm.internal.reftypes")

fun isBuiltInWasmRefType(type: IrType): Boolean {
    return type.classOrNull?.owner?.packageFqName == internalReftypesFqName
}

fun isExternalType(type: IrType): Boolean =
    type.erasedUpperBound.isExternal

fun IrType.getRuntimeClass(irBuiltIns: IrBuiltIns): IrClass =
    erasedUpperBound.takeIf { !it.isInterface } ?: irBuiltIns.anyClass.owner