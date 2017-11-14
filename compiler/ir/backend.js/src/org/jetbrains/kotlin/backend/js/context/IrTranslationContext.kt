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

package org.jetbrains.kotlin.backend.js.context

import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsProgramFragment
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.js.backend.ast.JsStatement

class IrTranslationContext(val staticContext: IrTranslationStaticContext, val fragment: JsProgramFragment) {
    var statements: MutableCollection<JsStatement> = mutableListOf()
    var scope: JsScope = staticContext.scope
        private set
    val module get() = staticContext.module

    fun addStatement(statement: JsStatement) {
        statements.add(statement)
    }

    inline fun <T> savingStatements(action: () -> T): T {
        val oldStatements = this.statements
        val result = action()
        this.statements = oldStatements
        return result
    }

    inline fun <T> withStatements(statements: MutableCollection<JsStatement>, action: () -> T): T = savingStatements {
        this.statements = statements
        action()
    }

    val names: Provider<IrSymbol, JsName>
        get() = staticContext.names
}