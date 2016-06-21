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

internal class KtAnnotationWrapper(val psi: KtAnnotationEntry) {
    val name: String
        get() = (psi.typeReference?.typeElement as? KtUserType)?.referencedName.orAnonymous()

    val valueArguments by lazy {
        psi.valueArguments.map {
            Pair(it.getArgumentName()?.toString(), convert(it.getArgumentExpression()!!))
        }
    }

    internal fun String?.orAnonymous(kind: String = ""): String {
        return this ?: "<anonymous" + (if (kind.isNotBlank()) " $kind" else "") + ">"
    }

    internal fun convert(expression: KtExpression): Any? = when (expression) {
        is KtStringTemplateExpression -> {
            if (expression.entries.isEmpty())
                ""
            else if (expression.entries.size == 1)
                convert(expression.entries[0])
            else
                ""
            // TODO: parse expressions, etc. e.g.:
            //      convertStringTemplateExpression(expression, parent, expression.entries.size - 1)
        }
        else -> null

    }

    internal fun convert(entry: KtStringTemplateEntry): Any? = when (entry) {
        is KtStringTemplateEntryWithExpression -> convertOrEmpty(entry.expression)
        is KtEscapeStringTemplateEntry -> entry.unescapedValue
        else -> {
            StringUtil.unescapeStringCharacters(entry.text)
        }
    }

    internal fun convertOrEmpty(expression: KtExpression?): Any? {
        return if (expression != null) convert(expression) else null
    }
}

fun parseAnnotation(ann: KtAnnotationEntry): Pair<String, Iterable<Any?>> {
    val wann = KtAnnotationWrapper(ann)
    val vals = wann.valueArguments
    return Pair(wann.name, vals)
}

