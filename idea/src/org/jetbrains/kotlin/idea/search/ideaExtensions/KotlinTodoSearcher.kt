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

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.cache.TodoCacheManager
import com.intellij.psi.search.IndexPattern
import com.intellij.psi.search.IndexPatternOccurrence
import com.intellij.psi.search.searches.IndexPatternSearch
import org.jetbrains.kotlin.compatibility.ExecutorProcessor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

data class KotlinTodoOccurrence(private val _file: PsiFile, private val _textRange: TextRange, private val _pattern: IndexPattern) :
    IndexPatternOccurrence {
    override fun getFile() = _file
    override fun getPattern() = _pattern
    override fun getTextRange() = _textRange
}

class KotlinTodoSearcher : QueryExecutorBase<IndexPatternOccurrence, IndexPatternSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: IndexPatternSearch.SearchParameters, consumer: ExecutorProcessor<IndexPatternOccurrence>) {
        var pattern = queryParameters.pattern
        if (pattern != null && !pattern.patternString.contains("TODO", true)) return
        if (pattern == null) {
            pattern = queryParameters.patternProvider.indexPatterns.firstOrNull { it.patternString.contains("TODO", true) } ?: return
        }

        val file = queryParameters.file

        val cacheManager = TodoCacheManager.SERVICE.getInstance(file.project)
        val patternProvider = queryParameters.patternProvider
        val count = if (patternProvider != null) {
            cacheManager.getTodoCount(file.virtualFile, patternProvider)
        } else
            cacheManager.getTodoCount(file.virtualFile, pattern)
        if (count == 0) return

        file.accept(object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                if (expression.calleeExpression?.text == "TODO") {
                    consumer.process(KotlinTodoOccurrence(file, expression.textRange, pattern))
                }
            }
        })
    }
}
