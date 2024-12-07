/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import java.util.Base64

fun String.toJsStringLiteral(): CharSequence =
    JsToStringGenerationVisitor.javaScriptString(this)

data class JsModuleAndQualifierReference(
    val module: String?,
    val qualifier: String?,
) {
    private val moduleBase64 = module?.let { encode(it) }.orEmpty()

    private val qualifierBase64 = qualifier?.let { encode(it) }.orEmpty()

    val jsVariableName = "_ref_${moduleBase64}_$qualifierBase64"

    val importVariableName = "_import_${moduleBase64}_$qualifierBase64"

    companion object {
        // Encode variable name as base64 to have a valid unique JS identifier
        private val encoder = Base64.getEncoder().withoutPadding()

        fun encode(value: String): String {
            return encoder.encodeToString(value.encodeToByteArray())
        }
    }
}