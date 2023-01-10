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
    val jsVariableName = run {
        // Encode variable name as base64 to have a valid unique JS identifier
        val encoder = Base64.getEncoder().withoutPadding()
        val moduleBase64 = module?.let { encoder.encodeToString(module.encodeToByteArray()) }.orEmpty()
        val qualifierBase64 = qualifier?.let { encoder.encodeToString(qualifier.encodeToByteArray()) }.orEmpty()
        "_ref_${moduleBase64}_$qualifierBase64"
    }
}