/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.script

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.psi.*

object SimpleUntypedAst {

    sealed class Node<out T>(val name: String) {
        class empty(name: String) : Node<Unit>(name)
        class str(name: String, val value: String) : Node<String>(name)
        class int(name: String, val value: Int) : Node<Int>(name)
        class list<out T>(name: String, val value: List<Node<T>>) : Node<List<Node<T>>>(name)
        class ann(name: String, val value: List<Node<Any>>) : Node<List<Node<Any>>>(name)
    }

    class KtAnnotationWrapper(val psi: KtAnnotationEntry) {
        val name: String
            get() = (psi.typeReference?.typeElement as? KtUserType)?.referencedName.orAnonymous()

        val valueArguments by lazy {
            psi.valueArguments.map {
                val name = it.getArgumentName()?.asName?.identifier.orAnonymous()
                convert(it.getArgumentExpression()!!)
            }
        }

        internal fun String?.orAnonymous(kind: String = ""): String {
            return this ?: "<anonymous" + (if (kind.isNotBlank()) " $kind" else "") + ">"
        }

        internal fun convert(expression: KtExpression): Node<Any> = when (expression) {
            is KtStringTemplateExpression -> {
                if (expression.entries.isEmpty())
                    SimpleUntypedAst.Node.str(name, "")
                else if (expression.entries.size == 1)
                    convert(expression.entries[0])
                else
                    SimpleUntypedAst.Node.str(name, "")
                    // TODO: parse expressions, etc. e.g.:
                    //      convertStringTemplateExpression(expression, parent, expression.entries.size - 1)
            }
            else -> Node.empty(name)

        }

        internal fun convert(entry: KtStringTemplateEntry): Node<Any> = when (entry) {
            is KtStringTemplateEntryWithExpression -> convertOrEmpty(entry.expression)
            is KtEscapeStringTemplateEntry -> Node.str(name, entry.unescapedValue)
            else -> {
                Node.str(name, StringUtil.unescapeStringCharacters(entry.text))
            }
        }

        internal fun convertOrEmpty(expression: KtExpression?): Node<Any> {
            return if (expression != null) convert(expression) else Node.empty(name)
        }
    }
}

fun parseAnnotation(ann: KtAnnotationEntry): SimpleUntypedAst.Node.list<Any> {
    val wann = SimpleUntypedAst.KtAnnotationWrapper(ann)
    val vals = wann.valueArguments
    return SimpleUntypedAst.Node.list(wann.name, vals)
}

