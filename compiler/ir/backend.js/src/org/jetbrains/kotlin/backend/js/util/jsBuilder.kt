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

package org.jetbrains.kotlin.backend.js.util

import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.sideEffects

class JsBuilder(private var currentSource: Any? = null) {
    fun <T : JsNode> withSource(source: Any?, action: JsBuilder.() -> T): T {
        val oldSource = currentSource
        currentSource = source
        val result = action()
        currentSource = oldSource
        return result
    }

    fun JsExpression.assign(rhs: JsExpression): JsBinaryOperation =
            attachSource(JsBinaryOperation(JsBinaryOperator.ASG, this, rhs))

    fun JsName.newVar(initializer: JsExpression? = null): JsVars = attachSource(JsVars(attachSource(JsVars.JsVar(this, initializer))))

    fun statement(expression: JsExpression): JsExpressionStatement = JsExpressionStatement(expression)

    fun JsExpression.dot(name: JsName) = attachSource(JsNameRef(name, this))

    fun JsName.dot(name: JsName) = ref().dot(name)

    fun JsExpression.dot(name: String) = attachSource(JsNameRef(name, this))

    fun JsName.dot(name: String) = ref().dot(name)

    fun String.dot(name: String) = ref().dot(name)

    fun JsExpression.dotPure(name: JsName) = dot(name).pure()

    fun JsName.dotPure(name: JsName) = ref().dotPure(name)

    fun String.dotPure(name: String) = ref().dotPure(name)

    fun JsExpression.dotPure(name: String) = dot(name).pure()

    fun JsName.dotPure(name: String) = ref().dotPure(name)

    fun JsName.ref() = attachSource(JsNameRef(this))

    fun String.ref() = attachSource(JsNameRef(this))

    fun String.str() = attachSource(JsStringLiteral(this))

    fun JsName.refPure() = ref().pure()

    fun String.refPure() = ref().pure()

    fun JsExpression.or(that: JsExpression) = attachSource(JsBinaryOperation(JsBinaryOperator.OR, this, that))

    fun not(arg: JsExpression) = attachSource(JsPrefixOperation(JsUnaryOperator.NOT, arg))

    fun JsExpression.invoke(vararg arguments: JsExpression) = attachSource(JsInvocation(this, *arguments))

    fun JsExpression.newInstance(vararg arguments: JsExpression) = attachSource(JsNew(this, arguments.toList()))

    fun emptyObject() = attachSource(JsObjectLiteral(false))

    fun JsExpression.dotPrototype() = dotPure("prototype")

    fun Int.literal() = JsIntLiteral(this)

    fun JsExpression.defineProperty(name: String, value: JsExpression): JsInvocation =
            JsInvocation("Object".dot("defineProperty"), this, name.str(), value)

    fun JsExpression.defineGetter(name: String, body: JsExpression): JsInvocation {
        val propertyLiteral = attachSource(JsObjectLiteral(true))
        propertyLiteral.propertyInitializers.add(JsPropertyInitializer("get".str(), body))
        return defineProperty(name, propertyLiteral)
    }

    fun <T : JsExpression> T.pure(): T = apply { sideEffects = SideEffectKind.PURE }

    fun <T : JsNode> attachSource(node: T): T {
        node.source = currentSource
        return node
    }
}

inline fun <T : JsNode> buildJs(source: Any?, action: JsBuilder.() -> T): T = JsBuilder(source).action()

inline fun <T : JsNode> buildJs(action: JsBuilder.() -> T): T = JsBuilder().action()

