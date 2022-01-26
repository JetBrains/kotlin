/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimization

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.ast.JsStatement
import org.jetbrains.kotlin.js.util.TextOutputImpl

class JsExternsGenerator(private val context: JsIrBackendContext) {
    fun generateExternsText(): String {
        return generateExterns()
            .run {
                val result = TextOutputImpl()
                JsToStringGenerationVisitor(result).acceptList(this)
                result.toString()
            }
    }

    private fun generateExterns(): List<JsStatement> {
        return context.externalDeclarations.flatMap { it.generateExtern() }
    }

    private fun IrDeclaration.generateExtern(): List<JsStatement> {
        TODO()
    }
}
