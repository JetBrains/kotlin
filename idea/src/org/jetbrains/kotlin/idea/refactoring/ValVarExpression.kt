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

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.Result
import com.intellij.codeInsight.template.TextResult

object ValVarExpression: Expression() {
    private val cachedLookupElements = listOf("val", "var").map { LookupElementBuilder.create(it) }.toTypedArray<LookupElement>()

    override fun calculateResult(context: ExpressionContext?): Result? = TextResult("val")

    override fun calculateQuickResult(context: ExpressionContext?): Result? = calculateResult(context)

    override fun calculateLookupItems(context: ExpressionContext?): Array<LookupElement>? = cachedLookupElements
}