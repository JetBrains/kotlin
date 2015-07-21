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

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.evaluation.CodeFragmentFactory
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.Semaphore
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup
import com.sun.jdi.ObjectReference
import com.sun.jdi.Value
import org.jetbrains.kotlin.asJava.KotlinLightClass
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.debugger.KotlinEditorTextProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.types.JetType
import java.util.HashMap
import java.util.concurrent.atomic.AtomicReference

class KotlinCodeFragmentFactory: CodeFragmentFactory() {
    private val LOG = Logger.getInstance(this.javaClass)

    override fun createCodeFragment(item: TextWithImports, context: PsiElement?, project: Project): JavaCodeFragment {
        val contextElement = getWrappedContextElement(project, context)
        if (contextElement == null) {
            LOG.warn("CodeFragment with null context created:\noriginalContext = ${context?.getElementTextWithContext()}")
        }
        val codeFragment = if (item.getKind() == CodeFragmentKind.EXPRESSION) {
            JetExpressionCodeFragment(
                    project,
                    "fragment.kt",
                    item.getText(),
                    item.getImports(),
                    contextElement
            )
        }
        else {
            JetBlockCodeFragment(
                    project,
                    "fragment.kt",
                    item.getText(),
                    item.getImports(),
                    contextElement
            )
        }

        codeFragment.putCopyableUserData(JetCodeFragment.RUNTIME_TYPE_EVALUATOR, {
            expression: JetExpression ->

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

    private fun getWrappedContextElement(project: Project, context: PsiElement?)
            = wrapContextIfNeeded(project, getContextElement(context))

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

    companion object {
        public val LABEL_VARIABLE_VALUE_KEY: Key<Value> = Key.create<Value>("_label_variable_value_key_")
        public val DEBUG_LABEL_SUFFIX: String = "_DebugLabel"

        fun getContextElement(elementAt: PsiElement?): PsiElement? {
            if (elementAt == null) return null

            if (elementAt is PsiCodeBlock) {
                return getContextElement(elementAt.getContext()?.getContext())
            }

            if (elementAt is KotlinLightClass) {
                return getContextElement(elementAt.getOrigin())
            }

            val containingFile = elementAt.getContainingFile()
            if (containingFile !is JetFile) return null

            val expressionAtOffset = PsiTreeUtil.findElementOfClassAtOffset(containingFile, elementAt.getTextOffset(), javaClass<JetExpression>(), false)
            if (expressionAtOffset != null && KotlinEditorTextProvider.isAcceptedAsCodeFragmentContext(elementAt)) {
                return expressionAtOffset
            }

            return KotlinEditorTextProvider.findExpressionInner(elementAt, true) ?: containingFile
        }

        //internal for tests
        fun createCodeFragmentForLabeledObjects(markupMap: Map<*, ValueMarkup>): Pair<String, Map<String, ObjectReference>> {
            val sb = StringBuilder()
            val labeledObjects = HashMap<String, ObjectReference>()
            for ((value, markup) in markupMap.entrySet()) {
                val labelName = markup.getText()
                if (!Name.isValidIdentifier(labelName)) continue

                val objectRef = value as ObjectReference

                val typeName = value.type().name()
                val labelNameWithSuffix = labelName + DEBUG_LABEL_SUFFIX
                sb.append("val ").append(labelNameWithSuffix).append(": ").append(typeName).append("? = null\n")

                labeledObjects.put(labelNameWithSuffix, objectRef)
            }
            sb.append("val _debug_context_val = 1")
            return sb.toString() to labeledObjects
        }
    }

    private fun wrapContextIfNeeded(project: Project, originalContext: PsiElement?): PsiElement? {
        val session = XDebuggerManager.getInstance(project).getCurrentSession() as? XDebugSessionImpl
                                            ?: return originalContext

        val markupMap = session.getValueMarkers()?.getAllMarkers()
        if (markupMap == null || markupMap.isEmpty()) return originalContext

        val (text, labels) = createCodeFragmentForLabeledObjects(markupMap)
        if (text.isEmpty()) return originalContext

        return createWrappingContext(text, labels, originalContext, project)
    }

    // internal for test
    fun createWrappingContext(
            newFragmentText: String,
            labels: Map<String, ObjectReference>,
            originalContext: PsiElement?,
            project: Project
    ): PsiElement? {
        val codeFragment = JetPsiFactory(project).createBlockCodeFragment(newFragmentText, originalContext)

        codeFragment.accept(object : JetTreeVisitorVoid() {
            override fun visitProperty(property: JetProperty) {
                val reference = labels.get(property.getName())
                if (reference != null) {
                    property.putUserData(LABEL_VARIABLE_VALUE_KEY, reference)
                }
            }
        })

        return getContextElement(codeFragment.findElementAt(codeFragment.getText().length() - 1))
    }
}