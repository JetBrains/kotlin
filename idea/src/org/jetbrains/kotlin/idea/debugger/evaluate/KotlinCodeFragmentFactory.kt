/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.concurrency.Semaphore
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup
import com.sun.jdi.ArrayReference
import com.sun.jdi.PrimitiveValue
import com.sun.jdi.Value
import org.jetbrains.annotations.TestOnly
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.debugger.KotlinEditorTextProvider
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.refactoring.j2kText
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.j2k.AfterConversionPass
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
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
                    initImports(item.imports),
                    contextElement
            )
        }
        else {
            KtBlockCodeFragment(
                    project,
                    "fragment.kt",
                    item.text,
                    initImports(item.imports),
                    contextElement
            )
        }

        codeFragment.putCopyableUserData(KtCodeFragment.RUNTIME_TYPE_EVALUATOR, {
            expression: KtExpression ->

            val debuggerContext = DebuggerManagerEx.getInstanceEx(project).context
            val debuggerSession = debuggerContext.debuggerSession
            if (debuggerSession == null ||  debuggerContext.suspendContext == null) {
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

        return codeFragment
    }

    private fun initImports(imports: String?): String? {
        if (imports != null && !imports.isEmpty()) {
            return imports.split(KtCodeFragment.IMPORT_SEPARATOR)
                    .mapNotNull { fixImportIfNeeded(it) }
                    .joinToString(KtCodeFragment.IMPORT_SEPARATOR)
        }
        return null
    }

    private fun fixImportIfNeeded(import: String): String? {
        // skip arrays
        if (import.endsWith("[]")) {
            return fixImportIfNeeded(import.removeSuffix("[]").trim())
        }

        // skip primitive types
        if (PsiTypesUtil.boxIfPossible(import) != import) {
            return null
        }
        return import
    }

    private fun getWrappedContextElement(project: Project, context: PsiElement?)
            = wrapContextIfNeeded(project, getContextElement(context))

    override fun createPresentationCodeFragment(item: TextWithImports, context: PsiElement?, project: Project): JavaCodeFragment {
        val kotlinCodeFragment = createCodeFragment(item, context, project)
        if (PsiTreeUtil.hasErrorElements(kotlinCodeFragment) && kotlinCodeFragment is KtExpressionCodeFragment) {
            val javaExpression = try {
                PsiElementFactory.SERVICE.getInstance(project).createExpressionFromText(item.text, context)
            }
            catch(e: IncorrectOperationException) {
                null
            }

            val importList = try {
                kotlinCodeFragment.importsAsImportList()?.let {
                    (PsiFileFactory.getInstance(project).createFileFromText(
                            "dummy.java", JavaFileType.INSTANCE, it.text
                    ) as? PsiJavaFile)?.importList
                }
            }
            catch(e: IncorrectOperationException) {
                null
            }

            if (javaExpression != null) {
                var convertedFragment: KtExpressionCodeFragment? = null
                project.executeWriteCommand("Convert java expression to kotlin in Evaluate Expression") {
                    val newText = javaExpression.j2kText()
                    val newImports = importList?.j2kText()
                    if (newText != null) {
                        convertedFragment = KtExpressionCodeFragment(
                                project,
                                kotlinCodeFragment.name,
                                newText,
                                newImports,
                                kotlinCodeFragment.context
                        )

                        AfterConversionPass(project, J2kPostProcessor(formatCode = false)).run(convertedFragment!!, range = null)
                    }
                }
                return convertedFragment ?: kotlinCodeFragment
            }
        }
        return kotlinCodeFragment
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
        val LABEL_VARIABLE_VALUE_KEY: Key<Value> = Key.create<Value>("_label_variable_value_key_")
        val DEBUG_LABEL_SUFFIX: String = "_DebugLabel"
        @TestOnly val DEBUG_FRAME_FOR_TESTS: Key<StackFrameProxyImpl> = Key.create("DEBUG_FRAME_FOR_TESTS")

        fun getContextElement(elementAt: PsiElement?): KtElement? {
            if (elementAt == null) return null

            if (elementAt is PsiCodeBlock) {
                return getContextElement(elementAt.context?.context)
            }

            if (elementAt is KtLightClass) {
                return getContextElement(elementAt.kotlinOrigin)
            }

            val containingFile = elementAt.containingFile
            if (containingFile !is KtFile) return null

            // elementAt can be PsiWhiteSpace when codeFragment is created from line start offset (in case of first opening EE window)
            val lineStartOffset = if (elementAt is PsiWhiteSpace || elementAt is PsiComment) {
                PsiTreeUtil.skipSiblingsForward(elementAt, PsiWhiteSpace::class.java, PsiComment::class.java)?.textOffset ?: elementAt.textOffset
            } else {
                elementAt.textOffset
            }

            var result = PsiTreeUtil.findElementOfClassAtOffset(containingFile, lineStartOffset, KtExpression::class.java, false)
            if (result.check()) {
                return CodeInsightUtils.getTopmostElementAtOffset(result!!, lineStartOffset, KtExpression::class.java)
            }

            result = KotlinEditorTextProvider.findExpressionInner(elementAt, true)
            if (result.check()) {
                return result
            }

            return containingFile
        }

        private fun KtElement?.check(): Boolean = this != null && this.check { KotlinEditorTextProvider.isAcceptedAsCodeFragmentContext(it) } != null

        //internal for tests
        fun createCodeFragmentForLabeledObjects(project: Project, markupMap: Map<*, ValueMarkup>): Pair<String, Map<String, Value>> {
            val sb = StringBuilder()
            val labeledObjects = HashMap<String, Value>()
            val psiNameHelper = PsiNameHelper.getInstance(project)

            val entrySet: Set<Map.Entry<*, ValueMarkup>> = markupMap.entries
            for ((value, markup) in entrySet) {
                val labelName = markup.text
                if (!psiNameHelper.isIdentifier(labelName)) continue

                val objectRef = value as? Value ?: continue

                val labelNameWithSuffix = "$labelName$DEBUG_LABEL_SUFFIX"
                sb.append("${createKotlinProperty(project, labelNameWithSuffix, objectRef.type().name(), objectRef)}\n")

                labeledObjects.put(labelNameWithSuffix, objectRef)
            }
            sb.append("val _debug_context_val = 1")
            return sb.toString() to labeledObjects
        }

        private fun createKotlinProperty(project: Project, variableName: String, variableTypeName: String, value: Value): String? {
            val actualClassDescriptor = value.asValue().asmType.getClassDescriptor(GlobalSearchScope.allScope(project))
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

        val markupMap = session.valueMarkers?.allMarkers
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

        return getContextElement(codeFragment.findElementAt(codeFragment.text.length - 1))
    }
}
