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
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.openapi.application.ApplicationManager
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
import com.sun.jdi.*
import org.jetbrains.annotations.TestOnly
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.kotlin.asJava.KtLightClass
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.refactoring.j2kText
import org.jetbrains.kotlin.idea.core.refactoring.quoteIfNeeded
import org.jetbrains.kotlin.idea.debugger.KotlinEditorTextProvider
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class KotlinCodeFragmentFactory: CodeFragmentFactory() {
    private val LOG = Logger.getInstance(this.javaClass)

    override fun createCodeFragment(item: TextWithImports, context: PsiElement?, project: Project): JavaCodeFragment {
        val contextElement = getWrappedContextElement(project, context)
        if (contextElement == null) {
            LOG.warn("CodeFragment with null context created:\noriginalContext = ${context?.getElementTextWithContext()}")
        }
        val codeFragment = if (item.kind == CodeFragmentKind.EXPRESSION) {
            KtExpressionCodeFragment(
                    project,
                    "fragment.kt",
                    item.text,
                    item.imports,
                    contextElement
            )
        }
        else {
            KtBlockCodeFragment(
                    project,
                    "fragment.kt",
                    item.text,
                    item.imports,
                    contextElement
            )
        }

        codeFragment.putCopyableUserData(KtCodeFragment.RUNTIME_TYPE_EVALUATOR, {
            expression: KtExpression ->

            val debuggerContext = DebuggerManagerEx.getInstanceEx(project).context
            val debuggerSession = debuggerContext.debuggerSession
            if (debuggerSession == null) {
                null
            }
            else {
                val semaphore = Semaphore()
                semaphore.down()
                val nameRef = AtomicReference<KotlinType>()
                val worker = object : KotlinRuntimeTypeEvaluator(null, expression, debuggerContext, ProgressManager.getInstance().progressIndicator) {
                    override fun typeCalculationFinished(type: KotlinType?) {
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

        if (contextElement != null) {
            val lambda = getInlineLambdaIfInside(contextElement)
            if (lambda != null) {
                codeFragment.putCopyableUserData(KtCodeFragment.ADDITIONAL_CONTEXT_FOR_LAMBDA, lamdba@ {
                    val debuggerContext = DebuggerManagerEx.getInstanceEx(project).context

                    val semaphore = Semaphore()
                    semaphore.down()

                    var visibleVariables: Map<LocalVariable, Value>? = null

                    val worker = object : DebuggerCommandImpl() {
                        override fun action() {
                            try {
                                val frame = if (ApplicationManager.getApplication().isUnitTestMode)
                                    context?.getCopyableUserData(DEBUG_FRAME_FOR_TESTS)?.stackFrame
                                else
                                    debuggerContext.frameProxy?.stackFrame

                                visibleVariables = frame?.let {
                                    f ->
                                    val variables = f.visibleVariables().filter {
                                        !it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT) &&
                                        !it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION)
                                    }
                                    f.getValues(variables)
                                } ?: emptyMap<LocalVariable, Value>()
                            }
                            catch(ignored: AbsentInformationException) {
                                // Debug info unavailable
                            }
                            finally {
                                semaphore.up()
                            }
                        }
                    }

                    debuggerContext.debugProcess?.managerThread?.invoke(worker)

                    for (i in 0..50) {
                        if (semaphore.waitFor(20)) break
                    }

                    if (visibleVariables == null) return@lamdba null

                    val fragmentForVisibleVariables = createCodeFragmentForVisibleVariables(lambda.project, visibleVariables!!)
                    return@lamdba createWrappingContext(
                            fragmentForVisibleVariables.first,
                            fragmentForVisibleVariables.second,
                            lambda.bodyExpression,
                            lambda.project)
                })
            }
        }

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
        return contextElement?.language == KotlinFileType.INSTANCE.language
    }

    override fun getFileType() = KotlinFileType.INSTANCE

    override fun getEvaluatorBuilder() = KotlinEvaluationBuilder

    companion object {
        public val LABEL_VARIABLE_VALUE_KEY: Key<Value> = Key.create<Value>("_label_variable_value_key_")
        public val DEBUG_LABEL_SUFFIX: String = "_DebugLabel"
        @TestOnly val DEBUG_FRAME_FOR_TESTS: Key<StackFrameProxyImpl> = Key.create("DEBUG_FRAME_FOR_TESTS")

        fun getContextElement(elementAt: PsiElement?): KtElement? {
            if (elementAt == null) return null

            if (elementAt is PsiCodeBlock) {
                return getContextElement(elementAt.context?.context)
            }

            if (elementAt is KtLightClass) {
                return getContextElement(elementAt.getOrigin())
            }

            val containingFile = elementAt.containingFile
            if (containingFile !is KtFile) return null

            var result = PsiTreeUtil.findElementOfClassAtOffset(containingFile, elementAt.textOffset, javaClass<KtExpression>(), false)
            if (result.check()) {
                return CodeInsightUtils.getTopmostElementAtOffset(result!!, result.textOffset, KtExpression::class.java)
            }

            result = KotlinEditorTextProvider.findExpressionInner(elementAt, true)
            if (result.check()) {
                return result
            }

            return containingFile
        }

        private fun KtElement?.check(): Boolean = this != null && this.check { KotlinEditorTextProvider.isAcceptedAsCodeFragmentContext(it) } != null

        private fun getInlineLambdaIfInside(contextElement: KtElement): KtFunction? {
            val parentLambda = contextElement.getParentOfType<KtFunction>(false) ?: return null

            if (!parentLambda.isLocal) return null
            if (KtPsiUtil.getParentCallIfPresent(parentLambda) == null) return null

            val bindingContext = contextElement.analyze(BodyResolveMode.PARTIAL)
            if (InlineUtil.isInlinedArgument(parentLambda, bindingContext, false))  {
                return parentLambda
            }
            return null
        }

        //internal for tests
        fun createCodeFragmentForLabeledObjects(project: Project, markupMap: Map<*, ValueMarkup>): Pair<String, Map<String, Value>> {
            val sb = StringBuilder()
            val labeledObjects = HashMap<String, Value>()
            val entrySet: Set<Map.Entry<*, ValueMarkup>> = markupMap.entrySet()
            for ((value, markup) in entrySet) {
                val labelName = markup.text
                if (!Name.isValidIdentifier(labelName)) continue

                val objectRef = value as? Value ?: continue

                val labelNameWithSuffix = "$labelName$DEBUG_LABEL_SUFFIX"
                sb.append("${createKotlinProperty(project, labelNameWithSuffix, objectRef.type().name(), objectRef)}\n")

                labeledObjects.put(labelNameWithSuffix, objectRef)
            }
            sb.append("val _debug_context_val = 1")
            return sb.toString() to labeledObjects
        }

        private fun createCodeFragmentForVisibleVariables(project: Project, visibleVariables: Map<LocalVariable, Value>): Pair<String, Map<String, Value>> {
            val sb = StringBuilder()
            val labeledObjects = HashMap<String, Value>()
            for ((variable, value) in visibleVariables) {
                val variableName = variable.name()
                if (!Name.isValidIdentifier(variableName)) continue

                val kotlinProperty = createKotlinProperty(project, variableName, variable.typeName(), value) ?: continue
                sb.append("$kotlinProperty\n")

                labeledObjects.put(variableName, value)
            }
            sb.append("val _debug_context_val = 1")
            return sb.toString() to labeledObjects
        }

        private fun createKotlinProperty(project: Project, variableName: String, variableTypeName: String, value: Value): String? {
            val actualClassDescriptor = value.asValue().asmType.getClassDescriptor(project)
            if (actualClassDescriptor != null && actualClassDescriptor.defaultType.arguments.isEmpty()) {
                val renderedType = IdeDescriptorRenderers.SOURCE_CODE.renderType(actualClassDescriptor.defaultType.makeNullable())
                return "val ${variableName.quoteIfNeeded()}: $renderedType = null"
            }

            fun String.addArraySuffix() = if (value is ArrayReference) this + "[]" else this

            val className = variableTypeName.replace("$", ".").substringBefore("[]")
            val classType = PsiType.getTypeByName(className, project, GlobalSearchScope.allScope(project))
            val type = (if (value !is PrimitiveValue && classType.resolve() == null)
                CommonClassNames.JAVA_LANG_OBJECT
            else
                className).addArraySuffix()

            val field = PsiElementFactory.SERVICE.getInstance(project).createField(variableName, PsiType.getTypeByName(type, project, GlobalSearchScope.allScope(project)))
            return field.j2kText()?.substringAfter("private ")
        }
    }

    private fun wrapContextIfNeeded(project: Project, originalContext: KtElement?): KtElement? {
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
    ): KtElement? {
        val codeFragment = KtPsiFactory(project).createBlockCodeFragment(newFragmentText, originalContext)

        codeFragment.accept(object : KtTreeVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                val reference = labels.get(property.name)
                if (reference != null) {
                    property.putUserData(LABEL_VARIABLE_VALUE_KEY, reference)
                }
            }
        })

        return getContextElement(codeFragment.findElementAt(codeFragment.text.length() - 1))
    }
}
