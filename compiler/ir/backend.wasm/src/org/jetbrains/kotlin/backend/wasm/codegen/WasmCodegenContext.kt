/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ast.WasmData
import org.jetbrains.kotlin.backend.wasm.ast.WasmModuleField
import org.jetbrains.kotlin.ir.backend.js.utils.Signature
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.parentAsClass


class WasmTypeInfo {
    val classes = mutableMapOf<IrClass, ClassMetadata>()
    val interfaces = mutableMapOf<IrClass, InterfaceMetadata>()
    val virtualFunctionIds = mutableMapOf<IrSimpleFunction, Int>()
    val signatures = mutableMapOf<Signature, VirtualMethodMetadata>()
    var datas = mutableListOf<WasmData>()
    var typeInfoSizeInBytes: Int = 0
}

class WasmCodegenContext(
    private val topLevelNames: Map<IrDeclarationWithName, String>,
    private val typeNames: Map<IrDeclarationWithName, String>,
    val backendContext: WasmBackendContext,
    val typeInfo: WasmTypeInfo
) {
    val imports = mutableListOf<WasmModuleField>()
    var localNames: Map<IrValueDeclaration, String> = emptyMap()
    var labels: Map<LoopLabel, String> = emptyMap()

    var currentClass: IrClass? = null

    val stringLiterals = mutableListOf<String>()

    fun getGlobalName(declaration: IrDeclarationWithName): String =
        topLevelNames[declaration]
            ?: error("Can't find name for ${declaration.fqNameWhenAvailable}")

    fun getLocalName(declaration: IrValueDeclaration): String =
        localNames[declaration]
            ?: error("Can't find local name for ${declaration.fqNameWhenAvailable}")

    fun getBreakLabelName(loop: IrLoop): String =
        labels[LoopLabel(loop, LoopLabelType.BREAK)]
            ?: error("Can't find break label name")

    fun getContinueLabelName(loop: IrLoop): String =
        labels[LoopLabel(loop, LoopLabelType.CONTINUE)]
            ?: error("Can't find continue label name")

    fun getLoopLabelName(loop: IrLoop): String =
        labels[LoopLabel(loop, LoopLabelType.LOOP)]
            ?: error("Can't find loop label name")

    fun getStructTypeName(klass: IrClass): String =
        typeNames[klass]
            ?: error("Can't find struct type name for class ${klass.fqNameWhenAvailable}")


    fun getFunctionTypeName(function: IrFunction): String =
        typeNames[function]
            ?: error("Can't find function type name ${function.fqNameWhenAvailable}")

}