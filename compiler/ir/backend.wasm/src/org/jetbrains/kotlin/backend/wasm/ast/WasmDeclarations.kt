/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ast

import org.jetbrains.kotlin.backend.wasm.utils.WasmImportPair

class WasmModule(
    val functionTypes: List<WasmFunctionType>,
    val structTypes: List<WasmStructType>,
    val importedFunctions: List<WasmImportedFunction>,
    val definedFunctions: List<WasmDefinedFunction>,
    val table: WasmTable,
    val memory: WasmMemory,
    val globals: List<WasmGlobal>,
    val exports: List<WasmExport>,
    val start: WasmStart?,
    val data: List<WasmData>
)

fun WasmModule.calculateIds() {
    functionTypes.calculateIds()
    structTypes.calculateIds(startIndex = functionTypes.size)
    importedFunctions.calculateIds()
    definedFunctions.calculateIds(startIndex = importedFunctions.size)
    globals.calculateIds()
}

fun List<WasmNamedModuleField>.calculateIds(startIndex: Int = 0) {
    for ((index, field) in this.withIndex()) {
        field.id = index + startIndex
    }
}

class WasmModuleOld(
    val fields: List<WasmModuleField>
)

open class WasmSymbol<Owner : Any> {
    constructor(owner: Owner) {
        ownerField = owner
    }

    private constructor()

    private var ownerField: Owner? = null

    fun bind(value: Owner) {
        ownerField = value
    }

    val owner: Owner
        get() = ownerField
            ?: error("Unbound wasm symbol $this")

    companion object {
        fun <Owner: Any> unbound() = WasmSymbol<Owner>()
    }
}

typealias WasmFunctionSymbol = WasmSymbol<WasmFunction>
typealias WasmGlobalSymbol = WasmSymbol<WasmGlobal>
typealias WasmFunctionTypeSymbol = WasmSymbol<WasmFunctionType>
typealias WasmStructTypeSymbol = WasmSymbol<WasmStructType>
typealias WasmStructFieldSymbol = WasmSymbol<Int>

sealed class WasmModuleField

sealed class WasmNamedModuleField(
    val name: String,
    val prefix: String
) : WasmModuleField() {
    var id: Int? = null
}

class WasmModuleFieldList(
    val fields: List<WasmModuleField>
) : WasmModuleField()

sealed class WasmFunction(
    name: String,
    val type: WasmFunctionType
) : WasmNamedModuleField(name, "fun")

class WasmDefinedFunction(
    name: String,
    type: WasmFunctionType,
    val locals: List<WasmLocal>,
    val instructions: List<WasmInstruction>
) : WasmFunction(name, type)

class WasmImportedFunction(
    name: String,
    type: WasmFunctionType,
    val importPair: WasmImportPair
) : WasmFunction(name, type)

class WasmMemory(
    val minSize: Int,
    val maxSize: Int?
) : WasmModuleField()

class WasmCustomSection(val wat: String) : WasmModuleField()

class WasmData(
    val offset: Int,
    val bytes: ByteArray
) : WasmModuleField()

class WasmTable(
    val functions: List<WasmFunction>
) : WasmModuleField()

class WasmParameter(
    val name: String,
    val type: WasmValueType
)

class WasmLocal(
    val name: String,
    val type: WasmValueType
)

class WasmGlobal(
    name: String,
    val type: WasmValueType,
    val isMutable: Boolean,
    val init: WasmInstruction?
) : WasmNamedModuleField(name, "g")

class WasmExport(
    val function: WasmFunction,
    val exportedName: String,
    val kind: Kind
) : WasmModuleField() {
    enum class Kind(val keyword: String) {
        FUNCTION("func"),
        GLOBAL("global")
    }
}

// Start function
class WasmStart(val ref: WasmFunction) : WasmModuleField()

sealed class WasmNamedType(name: String) :
    WasmNamedModuleField(name, "type")

class WasmFunctionType(
    name: String,
    val parameterTypes: List<WasmValueType>,
    val resultType: WasmValueType?
) : WasmNamedType(name)

class WasmStructType(
    name: String,
    val fields: List<WasmStructField>
) : WasmNamedType(name)

class WasmStructField(
    val name: String,
    val type: WasmValueType,
    val isMutable: Boolean
)