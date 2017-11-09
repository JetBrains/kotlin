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

package org.jetbrains.kotlin.idea.codeInsight.surroundWith.expression

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.statement.KotlinTrySurrounderBase
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTryExpression

sealed class KotlinTryExpressionSurrounder : KotlinControlFlowExpressionSurrounderBase() {
    class TryCatch : KotlinTryExpressionSurrounder() {
        override fun getTemplateDescription() = "try { expr } catch {}"
        override fun getPattern() = "try { $0 } catch (e: Exception) {}"
    }

    class TryCatchFinally : KotlinTryExpressionSurrounder() {
        override fun getTemplateDescription() = "try { expr } catch {} finally {}"
        override fun getPattern() = "try { $0 } catch (e: Exception) {} finally {}"
    }

    override fun getRange(editor: Editor, replaced: KtExpression): TextRange? {
        val tryExpression = replaced as KtTryExpression
        return KotlinTrySurrounderBase.getCatchTypeParameterTextRange(tryExpression)
    }
}

