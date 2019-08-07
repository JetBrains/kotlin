/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ast.WasmModuleField
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable

class WasmCodegenContext(
    private val topLevelNames: Map<IrDeclarationWithName, String>,
    val backendContext: WasmBackendContext
) {
    val imports = mutableListOf<WasmModuleField>()
    var localNames: Map<IrValueDeclaration, String> = emptyMap()
    val stringLiterals = mutableListOf<String>()

    fun getGlobalName(declaration: IrDeclarationWithName): String =
        topLevelNames[declaration]
            ?: error("Can't find name for ${declaration.fqNameWhenAvailable}")

    fun getLocalName(declaration: IrValueDeclaration): String =
        localNames[declaration]
            ?: error("Can't find local name for ${declaration.fqNameWhenAvailable}")
}