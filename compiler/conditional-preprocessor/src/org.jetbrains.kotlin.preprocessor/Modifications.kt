/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.preprocessor

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.psi.*

data class Modification(val range: TextRange, val apply: (String) -> String)

class CollectModificationsVisitor(evaluators: List<Evaluator>) : JetTreeVisitorVoid() {

    val elementModifications: Map<Evaluator, MutableList<Modification>> =
            evaluators.toMap(selector = { it }, transform = { arrayListOf<Modification>() })

    override fun visitDeclaration(declaration: JetDeclaration) {
        super.visitDeclaration(declaration)

        val annotations = declaration.parseConditionalAnnotations()
        val name = (declaration as? JetNamedDeclaration)?.nameAsSafeName ?: declaration.name

        val declResults = arrayListOf<Pair<Evaluator, Boolean>>()
        for ((evaluator, modifications) in elementModifications) {
            val conditionalResult = evaluator(annotations)
            declResults.add(evaluator to conditionalResult)

            if (!conditionalResult)
                modifications.add(Modification(declaration.textRange) { rangeText ->
                    StringBuilder {
                        append("/* Not available on $evaluator */")
                        repeat(StringUtil.getLineBreakCount(rangeText)) { append("\n") }
                    }.toString()
                })
            else {
                val targetName = annotations.filterIsInstance<Conditional.TargetName>().singleOrNull()
                if (targetName != null) {
                    val placeholderName = (declaration as JetNamedDeclaration).nameAsName!!.asString()
                    val realName = targetName.name
                    modifications.add(Modification(declaration.textRange) { it.replace(placeholderName, realName) })
                }
            }

        }
        //println("declaration: ${declaration.javaClass.simpleName} $name${if (annotations.isNotEmpty()) ", annotations: ${annotations.joinToString { it.toString() }}, evaluation result: $declResults" else ""}")
    }
}

fun List<Modification>.applyTo(sourceText: String): String {
    return StringBuilder {
        var prevIndex = 0
        for ((range, transform) in this@applyTo) {
            append(sourceText, prevIndex, range.startOffset)
            append(transform(range.substring(sourceText)))
            prevIndex = range.endOffset
        }
        append(sourceText, prevIndex, sourceText.length())
    }.toString()
}