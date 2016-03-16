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

package org.jetbrains.kotlin.idea.liveTemplates.macro

import com.intellij.codeInsight.template.*
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.liveTemplates.KotlinTemplateContextType
import org.jetbrains.kotlin.psi.KtFunction

class FunctionNameMacro : Macro() {
    override fun getName() = "functionName"
    override fun getPresentableName() = "functionName()"

    override fun calculateResult(params: Array<Expression>, context: ExpressionContext): Result? {
        val project = context.project
        val templateStartOffset = context.templateStartOffset
        val offset = if (templateStartOffset > 0) context.templateStartOffset - 1 else context.templateStartOffset

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val file = PsiDocumentManager.getInstance(project).getPsiFile(context.editor!!.document) ?: return null
        var place = file.findElementAt(offset)
        while (place != null) {
            if (place is KtFunction) {
                return TextResult(place.name!!)
            }
            place = place.parent
        }
        return null
    }

    override fun isAcceptableInContext(context: TemplateContextType?): Boolean {
        return context is KotlinTemplateContextType
    }

}