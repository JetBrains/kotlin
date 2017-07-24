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

import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.js.backend.ast.*

fun List<JsStatement>.asSingleStatement() = when (size) {
    0 -> JsEmpty
    1 -> first()
    else -> JsBlock(this)
}

fun IrTranslationContext.translateStatement(action: () -> Unit): JsStatement {
    val statements = mutableListOf<JsStatement>()
    withStatements(statements) {
        action()
    }
    return statements.asSingleStatement()
}

fun JsNode.replaceContinueWithBreak(name: JsName) {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(x: JsContinue, ctx: JsContext<in JsNode>) {
            if (x.label?.name == name) {
                ctx.replaceMe(JsBreak(name.makeRef()))
            }
        }
    }
    visitor.accept(this)
}