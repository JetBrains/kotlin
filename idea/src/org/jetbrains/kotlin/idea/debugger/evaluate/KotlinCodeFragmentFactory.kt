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
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.Semaphore
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup
import com.sun.jdi.ArrayReference
import com.sun.jdi.ObjectReference
import com.sun.jdi.PrimitiveValue
import com.sun.jdi.Value
import org.jetbrains.kotlin.asJava.KotlinLightClass
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.core.refactoring.j2kText
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
        val codeFragment = if (item.kind == CodeFragmentKind.EXPRESSION) {
            JetExpressionCodeFragment(
                    project,
                    "fragment.kt",
                    item.text,
                    item.imports,
                    contextElement
            )
        }
        else {
            JetBlockCodeFragment(
                    project,
                    "fragment.kt",
                    item.text,
                    item.imports,
                    contextElement
            )
        }

        codeFragment.putCopyableUserData(JetCodeFragment.RUNTIME_TYPE_EVALUATOR, {
            expression: JetExpression ->

            val debuggerContext = DebuggerManagerEx.getInstanceEx(project).context
            val debuggerSession = debuggerContext.debuggerSession
            if (debuggerSession == null) {
                null
            }
            else {
                val semaphore = Semaphore()
                semaphore.down()
                val nameRef = AtomicReference<JetType>()
                val worker = object : KotlinRuntimeTypeEvaluator(null, expression, debuggerContext, ProgressManager.getInstance().progressIndicator) {
                    override fun typeCalculationFinished(type: JetType?) {
                        nameRef.set(type)
                        semaphore.up()
                    }
                }

                debuggerContext.debugProcess?.managerThread?.invoke(worker)

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
            return isContextAccepted(contextElement.context?.context)
        }
        return contextElement?.language == JetFileType.INSTANCE.language
    }

    override fun getFileType() = JetFileType.INSTANCE

    override fun getEvaluatorBuilder() = KotlinEvaluationBuilder

    companion object {
        public val LABEL_VARIABLE_VALUE_KEY: Key<Value> = Key.create<Value>("_label_variable_value_key_")
        public val DEBUG_LABEL_SUFFIX: String = "_DebugLabel"

        fun getContextElement(elementAt: PsiElement?): JetElement? {
            if (elementAt == null) return null

            if (elementAt is PsiCodeBlock) {
                return getContextElement(elementAt.context?.context)
            }

            if (elementAt is KotlinLightClass) {
                return getContextElement(elementAt.getOrigin())
            }

            val containingFile = elementAt.containingFile
            if (containingFile !is JetFile) return null

            val expressionAtOffset = PsiTreeUtil.findElementOfClassAtOffset(containingFile, elementAt.textOffset, javaClass<JetExpression>(), false)
            if (expressionAtOffset != null && KotlinEditorTextProvider.isAcceptedAsCodeFragmentContext(elementAt)) {
                return expressionAtOffset
            }

            return KotlinEditorTextProvider.findExpressionInner(elementAt, true) ?: containingFile
        }

        //internal for tests
        fun createCodeFragmentForLabeledObjects(project: Project, markupMap: Map<*, ValueMarkup>): Pair<String, Map<String, Value>> {
            val sb = StringBuilder()
            val labeledObjects = HashMap<String, Value>()
            for ((value, markup) in markupMap.entrySet()) {
                val labelName = markup.text
                if (!Name.isValidIdentifier(labelName)) continue

                val objectRef = value as? Value ?: continue

                val labelNameWithSuffix = "$labelName$DEBUG_LABEL_SUFFIX"
                sb.append("${createKotlinProperty(project, labelNameWithSuffix, objectRef.type().name(), TypeKind.getTypeKind(objectRef))}\n")

                labeledObjects.put(labelNameWithSuffix, objectRef)
            }
            sb.append("val _debug_context_val = 1")
            return sb.toString() to labeledObjects
        }

        private enum class TypeKind {
            PRIMITIVE,
            ARRAY,
            OBJECT;

            companion object {
                fun getTypeKind(value: Value): TypeKind {
                    return when(value) {
                        is ArrayReference -> ARRAY
                        is PrimitiveValue -> PRIMITIVE
                        else -> OBJECT
                    }
                }
            }
        }

        private fun createKotlinProperty(project: Project, variableName: String, variableTypeName: String, typeKind: TypeKind): String? {
            fun String.addArraySuffix() = if (typeKind == TypeKind.ARRAY) this + "[]" else this

            val className = variableTypeName.replace("$", ".").substringBefore("[]")
            val classType = PsiType.getTypeByName(className, project, GlobalSearchScope.allScope(project))
            val type = (if (typeKind != TypeKind.PRIMITIVE && classType.resolve() == null)
                CommonClassNames.JAVA_LANG_OBJECT
            else
                className).addArraySuffix()

            val field = PsiElementFactory.SERVICE.getInstance(project).createField(variableName, PsiType.getTypeByName(type, project, GlobalSearchScope.allScope(project)))
            return field.j2kText()?.substringAfter("private ")
        }
    }

    private fun wrapContextIfNeeded(project: Project, originalContext: JetElement?): JetElement? {
        val session = XDebuggerManager.getInstance(project).currentSession as? XDebugSessionImpl
                                            ?: return originalContext

        val markupMap = session.valueMarkers?.getAllMarkers()
        if (markupMap == null || markupMap.isEmpty()) return originalContext

        val (text, labels) = createCodeFragmentForLabeledObjects(project, markupMap)
        if (text.isEmpty()) return originalContext

        return createWrappingContext(text, labels, originalContext, project)
    }

    // internal for test
    fun createWrappingContext(
            newFragmentText: String,
            labels: Map<String, Value>,
            originalContext: PsiElement?,
            project: Project
    ): JetElement? {
        val codeFragment = JetPsiFactory(project).createBlockCodeFragment(newFragmentText, originalContext)

        codeFragment.accept(object : JetTreeVisitorVoid() {
            override fun visitProperty(property: JetProperty) {
                val reference = labels.get(property.name)
                if (reference != null) {
                    property.putUserData(LABEL_VARIABLE_VALUE_KEY, reference)
                }
            }
        })

        return getContextElement(codeFragment.findElementAt(codeFragment.text.length() - 1))
    }
}