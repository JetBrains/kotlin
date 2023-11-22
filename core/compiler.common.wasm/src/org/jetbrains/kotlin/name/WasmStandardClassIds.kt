/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

import org.jetbrains.kotlin.name.StandardClassIds.BASE_KOTLIN_PACKAGE
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsModule
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsQualifier

object WasmStandardClassIds {
    val BASE_WASM_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("wasm"))

    object Annotations {
        @JvmField
        val WasmImport = "WasmImport".wasmId()

        @JvmField
        val WasmExport = "WasmExport".wasmId()

        @JvmField
        val JsFun = "JsFun".baseId()

        @JvmField
        val annotationsRequiringExternal = setOf(JsModule, JsQualifier)
    }
}

private fun String.baseId() = ClassId(BASE_KOTLIN_PACKAGE, Name.identifier(this))

private fun String.wasmId() = ClassId(WasmStandardClassIds.BASE_WASM_PACKAGE, Name.identifier(this))