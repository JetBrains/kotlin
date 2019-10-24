/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaDebuggerEvaluator
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.openapi.util.text.StringUtil
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XValue
import com.intellij.xdebugger.impl.evaluate.quick.XValueHint
import com.intellij.xdebugger.impl.ui.tree.nodes.EvaluatingExpressionRootNode
import com.intellij.xdebugger.impl.ui.tree.nodes.WatchNodeImpl
import java.util.*

private data class Key(val text: String, val imports: String) {
    constructor(expr: XExpression) : this(expr.expression, StringUtil.notNullize(expr.customInfo))
    constructor(text: TextWithImports) : this(text.text, text.imports)
}

class KotlinDebuggerEvaluator(
    debugProcess: DebugProcessImpl?,
    stackFrame: JavaStackFrame?
) : JavaDebuggerEvaluator(debugProcess, stackFrame) {
    private val types = HashMap<Key, EvaluationType>()

    fun getType(text: TextWithImports): EvaluationType {
        return types[Key(text)] ?: EvaluationType.UNKNOWN
    }

    override fun evaluate(expression: XExpression, callback: XEvaluationCallback, expressionPosition: XSourcePosition?) {
        val key = Key(expression)
        val type = getType(callback)
        if (type != null) {
            types[key] = type
        }

        val wrappedCallback = object : XEvaluationCallback {
            override fun errorOccurred(errorMessage: String) {
                types.remove(key)
                callback.errorOccurred(errorMessage)
            }

            override fun evaluated(result: XValue) {
                types.remove(key)
                callback.evaluated(result)
            }
        }

        super.evaluate(expression, wrappedCallback, expressionPosition)
    }

    private fun getType(callback: XEvaluationCallback): EvaluationType? {
        val name = callback.javaClass.name
        for (value in EvaluationType.values()) {
            val clazz = value.clazz ?: continue
            if (name.startsWith(clazz.name)) {
                return value
            }
        }

        return null
    }

    @Suppress("unused")
    enum class EvaluationType(val clazz: Class<*>?) {
        WATCH(WatchNodeImpl::class.java),
        WINDOW(EvaluatingExpressionRootNode::class.java),
        POPUP(XValueHint::class.java),
        FROM_JAVA(null),
        UNKNOWN(null);
    }
}