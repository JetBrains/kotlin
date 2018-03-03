/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.js.translate.context

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.backend.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.types.KotlinType

open class TemporaryVariable(
    private val variableName: JsName,
    private val assignmentExpression: JsExpression?,
    private val type: KotlinType?
) {

    fun reference(): JsNameRef {
        val result = variableName.makeRef()
        result.synthetic = true
        result.type = type
        return result
    }

    fun name(): JsName {
        return variableName
    }

    fun assignmentExpression(): JsExpression {
        return assignmentExpression!!
    }

    fun assignmentStatement(): JsStatement {
        return JsAstUtils.asSyntheticStatement(assignmentExpression())
    }

    companion object {

        @JvmStatic
        /*package*/ fun create(temporaryName: JsName, initExpression: JsExpression?): TemporaryVariable {
            var rhs: JsBinaryOperation? = null
            var type: KotlinType? = null
            if (initExpression != null) {
                rhs = JsAstUtils.assignment(temporaryName.makeRef(), initExpression)
                rhs.source(initExpression.source)
                rhs.synthetic = true
                type = initExpression.type
            }
            return TemporaryVariable(temporaryName, rhs, type)
        }
    }
}

