/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

/**
 * Consult KDoc of [StandardClassIds] regarding naming rules when adding declarations here.
 * Also, check if the desired id is already present there.
 */
object WasmStandardClassIds {
    val BASE_WASM_PACKAGE = StandardClassIds.BASE_KOTLIN_PACKAGE.child(Name.identifier("wasm"))
    val BASE_WASM_INTERNAL_PACKAGE = BASE_WASM_PACKAGE.child(Name.identifier("internal"))

    object Annotations {
        val WasmImport = "WasmImport".wasmId()
        val WasmExport = "WasmExport".wasmId()

        val JsFun = "JsFun".baseId()
        val EagerInitialization = "EagerInitialization".baseId()

        val annotationsRequiringExternal = setOf(
            WebCommonStandardClassIds.Annotations.JsModule,
            WebCommonStandardClassIds.Annotations.JsQualifier,
        )
    }
}

private fun String.baseId() = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier(this))
private fun String.wasmId() = ClassId(WasmStandardClassIds.BASE_WASM_PACKAGE, Name.identifier(this))
