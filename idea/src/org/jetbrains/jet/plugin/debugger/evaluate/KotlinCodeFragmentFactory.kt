/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger.evaluate

import com.intellij.debugger.engine.evaluation.CodeFragmentFactory
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaCodeFragment
import org.jetbrains.jet.plugin.JetFileType
import org.jetbrains.jet.lang.psi.JetExpressionCodeFragment
import com.intellij.psi.PsiCodeBlock
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.psi.JavaCodeFragmentFactory
import org.jetbrains.jet.plugin.debugger.KotlinEditorTextProvider
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetBlockCodeFragment

class KotlinCodeFragmentFactory: CodeFragmentFactory() {
    override fun createCodeFragment(item: TextWithImports, context: PsiElement?, project: Project): JavaCodeFragment {
        val codeFragment = if (item.getKind() == CodeFragmentKind.EXPRESSION) {
            JetExpressionCodeFragment(project, "fragment.kt", item.getText(), getContextElement(context))
        }
        else {
            JetBlockCodeFragment(project, "fragment.kt", item.getText(), getContextElement(context))
        }
        codeFragment.addImportsFromString(item.getImports())
        return codeFragment
    }

    override fun createPresentationCodeFragment(item: TextWithImports, context: PsiElement?, project: Project): JavaCodeFragment {
        return createCodeFragment(item, context, project)
    }

    override fun isContextAccepted(contextElement: PsiElement?): Boolean {
        if (contextElement is PsiCodeBlock) {
            // PsiCodeBlock -> DummyHolder -> originalElement
            return isContextAccepted(contextElement.getContext()?.getContext())
        }
        return contextElement?.getLanguage() == JetFileType.INSTANCE.getLanguage()
    }

    override fun getFileType() = JetFileType.INSTANCE

    override fun getEvaluatorBuilder() = KotlinEvaluationBuilder

    class object {
        fun getContextElement(elementAt: PsiElement?): PsiElement? {
            if (elementAt == null) return null

            if (elementAt is PsiCodeBlock) {
                return getContextElement(elementAt.getContext()?.getContext())
            }

            val expressionAtOffset = PsiTreeUtil.findElementOfClassAtOffset(elementAt.getContainingFile()!!, elementAt.getTextOffset(), javaClass<JetExpression>(), false)
            if (expressionAtOffset != null) {
                return expressionAtOffset
            }
            return KotlinEditorTextProvider.findExpressionInner(elementAt)
        }
    }
}
