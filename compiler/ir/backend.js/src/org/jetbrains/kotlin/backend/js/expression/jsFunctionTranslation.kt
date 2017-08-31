/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.js.expression

import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.parser.parse

fun IrTranslationContext.translateJsFunction(call: IrCall): JsExpression? {
    val argument = (call.getValueArgument(0) as? IrConst<*>)?.value as? String ?: return null

    val statements = parseJsCode(argument)
    return when (statements.size) {
        0 -> {
            JsNullLiteral()
        }
        1 -> {
            val resultStatement = statements[0]
            if (resultStatement is JsExpressionStatement) {
                resultStatement.expression
            }
            else {
                addStatement(resultStatement)
                null
            }
        }
        else -> {
            addStatements(statements)
            null
        }
    }
}


private fun parseJsCode(jsCode: String): List<JsStatement> {
    val temporaryRootScope = JsRootScope(JsProgram())
    return parse(jsCode, ThrowExceptionOnErrorReporter, temporaryRootScope, "")
}