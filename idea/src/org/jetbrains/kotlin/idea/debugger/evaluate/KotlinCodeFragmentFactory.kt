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

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.engine.evaluation.CodeFragmentFactory
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaCodeFragment
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.psi.JetExpressionCodeFragment
import com.intellij.psi.PsiCodeBlock
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import org.jetbrains.kotlin.idea.debugger.KotlinEditorTextProvider
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetBlockCodeFragment
import org.jetbrains.kotlin.asJava.KotlinLightClass
import com.intellij.debugger.DebuggerManagerEx
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.types.JetType
import com.intellij.util.concurrency.Semaphore
import java.util.concurrent.atomic.AtomicReference
import com.intellij.openapi.progress.ProgressManager

class KotlinCodeFragmentFactory: CodeFragmentFactory() {
    override fun createCodeFragment(item: TextWithImports, context: PsiElement?, project: Project): JavaCodeFragment {
        val codeFragment = if (item.getKind() == CodeFragmentKind.EXPRESSION) {
            JetExpressionCodeFragment(project, "fragment.kt", item.getText(), item.getImports(), getContextElement(context))
        }
        else {
            JetBlockCodeFragment(project, "fragment.kt", item.getText(), item.getImports(), getContextElement(context))
        }

        codeFragment.putCopyableUserData(JetCodeFragment.RUNTIME_TYPE_EVALUATOR, {
            (expression: JetExpression): JetType? ->

            val debuggerContext = DebuggerManagerEx.getInstanceEx(project).getContext()
            val debuggerSession = debuggerContext.getDebuggerSession()
            if (debuggerSession == null) {
                null
            }
            else {
                val semaphore = Semaphore()
                semaphore.down()
                val nameRef = AtomicReference<JetType>()
                val worker = object : KotlinRuntimeTypeEvaluator(null, expression, debuggerContext, ProgressManager.getInstance().getProgressIndicator()) {
                    override fun typeCalculationFinished(type: JetType?) {
                        nameRef.set(type)
                        semaphore.up()
                    }
                }

                debuggerContext.getDebugProcess()?.getManagerThread()?.invoke(worker)

                for (i in 0..50) {
                    ProgressManager.checkCanceled()
                    if (semaphore.waitFor(20)) break
                }

                nameRef.get()
            }
        })

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

            if (elementAt is KotlinLightClass) {
                return getContextElement(elementAt.getOrigin())
            }

            val expressionAtOffset = PsiTreeUtil.findElementOfClassAtOffset(elementAt.getContainingFile()!!, elementAt.getTextOffset(), javaClass<JetExpression>(), false)
            if (expressionAtOffset != null) {
                return expressionAtOffset
            }
            return KotlinEditorTextProvider.findExpressionInner(elementAt, true)
        }
    }
}
