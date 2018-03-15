/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.js.translate.context

import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsName

class TemporaryConstVariable(variableName: JsName, assignmentExpression: JsExpression): TemporaryVariable(variableName, assignmentExpression, null) {
    private var initialized = false

    fun value(): JsExpression {
        if (initialized) {
            return reference()
        }
        initialized = true
        return assignmentExpression()
    }
}
