/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.js.backend.ast.*

class FunctionWithJsFuncAnnotationInliner(private val jsFuncCall: IrCall, private val context: JsGenerationContext) {
    private val function = getJsFunctionImplementation()
    private val replacements = collectReplacementsForCall()

    fun generateResultStatement(): List<JsStatement> =
        function.body.statements.also {
            JsNameRemappingTransformer(replacements).apply { acceptList(it) }
        }

    private fun getJsFunctionImplementation(): JsFunction =
        context.staticContext.backendContext.getJsCodeForFunction(jsFuncCall.symbol)?.deepCopy()
            ?: compilationException("JS function not found", jsFuncCall)

    private fun collectReplacementsForCall(): Map<JsName, JsExpression> {
        val translatedArguments = jsFuncCall.arguments.map { it!!.accept(IrElementToJsExpressionTransformer(), context) }
        return function.parameters
            .map { it.name }
            .zip(translatedArguments)
            .toMap()
    }
}

private class JsNameRemappingTransformer(private val replacements: Map<JsName, JsExpression>) : JsVisitorWithContextImpl() {
    private val JsName.replacement: JsExpression? get() = replacements[this]

    override fun visit(nameRef: JsNameRef, ctx: JsContext<JsNode>): Boolean {
        super.visit(nameRef, ctx)
        if (nameRef.qualifier != null) return true
        val replacement = nameRef.name?.replacement ?: return true
        if (replacement.source == null) {
            replacement.source = nameRef.source
        }
        ctx.replaceMe(replacement)
        return false
    }
}
