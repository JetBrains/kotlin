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
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
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
import com.sun.jdi.*
import org.jetbrains.annotations.TestOnly
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.debugger.KotlinEditorTextProvider
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.refactoring.j2k
import org.jetbrains.kotlin.idea.refactoring.j2kText
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.versions.getKotlinJvmRuntimeMarkerClass
import org.jetbrains.kotlin.j2k.AfterConversionPass
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import java.util.concurrent.atomic.AtomicReference

class KotlinCodeFragmentFactory: CodeFragmentFactory() {
    private val LOG = Logger.getInstance(this::class.java)

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

        if (contextElement != null && contextElement !is KtElement) {
            codeFragment.putCopyableUserData(KtCodeFragment.FAKE_CONTEXT_FOR_JAVA_FILE, {
                val emptyFile = createFakeFileWithJavaContextElement("", contextElement)

                val debuggerContext = DebuggerManagerEx.getInstanceEx(project).context
                val debuggerSession = debuggerContext.debuggerSession
                if ((debuggerSession == null || debuggerContext.suspendContext == null) && !ApplicationManager.getApplication().isUnitTestMode) {
                    LOG.warn("Couldn't create fake context element for java file, debugger isn't paused on breakpoint")
                    return@putCopyableUserData emptyFile
                }

                val frameDescriptor = getFrameInfo(contextElement, debuggerContext)
                if (frameDescriptor == null) {
                    LOG.warn("Couldn't get info about 'this' and local variables for ${debuggerContext.sourcePosition.file.name}:${debuggerContext.sourcePosition.line}")
                    return@putCopyableUserData emptyFile
                }

                val receiverTypeReference = frameDescriptor.thisObject?.let { createKotlinProperty(project, "this_0", it.type().name(), it) }?.typeReference
                val receiverTypeText = receiverTypeReference?.let { "${it.text}." } ?: ""

                val kotlinVariablesText = frameDescriptor.visibleVariables.entries.associate { it.key.name() to it.value }.kotlinVariablesAsText(project)

                val fakeFunctionText = "fun ${receiverTypeText}_java_locals_debug_fun_() {\n$kotlinVariablesText\n}"

                val fakeFile = createFakeFileWithJavaContextElement(fakeFunctionText, contextElement)
                val fakeFunction = fakeFile.declarations.firstOrNull() as? KtFunction
                val fakeContext = (fakeFunction?.bodyExpression as? KtBlockExpression)?.statements?.lastOrNull()

                return@putCopyableUserData wrapContextIfNeeded(project, contextElement, fakeContext) ?: emptyFile

            })
        }

        return codeFragment
    }

    private fun getFrameInfo(contextElement: PsiElement?, debuggerContext: DebuggerContextImpl): FrameInfo? {
        val semaphore = Semaphore()
        semaphore.down()

        var frameInfo: FrameInfo? = null

        val worker = object : DebuggerCommandImpl() {
            override fun action() {
                try {
                    val frame = if (ApplicationManager.getApplication().isUnitTestMode)
                        contextElement?.getCopyableUserData(DEBUG_CONTEXT_FOR_TESTS)?.frameProxy?.stackFrame
                    else
                        debuggerContext.frameProxy?.stackFrame

                    val visibleVariables = frame?.let {
                        val values = it.getValues(it.visibleVariables())
                        values.filterValues { it != null }
                    } ?: emptyMap()

                    frameInfo = FrameInfo(frame?.thisObject(), visibleVariables)
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

        return frameInfo
    }

    private class FrameInfo(val thisObject: Value?, val visibleVariables: Map<LocalVariable, Value>)

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

    private fun getWrappedContextElement(project: Project, context: PsiElement?): PsiElement? {
        val newContext = getContextElement(context)
        if (newContext !is KtElement) return newContext
        return wrapContextIfNeeded(project, context, newContext)
    }

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
        return when {
        // PsiCodeBlock -> DummyHolder -> originalElement
            contextElement is PsiCodeBlock -> isContextAccepted(contextElement.context?.context)
            contextElement == null -> false
            contextElement.language == KotlinFileType.INSTANCE.language -> true
            contextElement.language == JavaFileType.INSTANCE.language -> {
                getKotlinJvmRuntimeMarkerClass(contextElement.project, contextElement.resolveScope) != null
            }
            else -> false
        }
    }

    override fun getFileType() = KotlinFileType.INSTANCE

    override fun getEvaluatorBuilder() = KotlinEvaluationBuilder

    companion object {
        val LABEL_VARIABLE_VALUE_KEY: Key<Value> = Key.create<Value>("_label_variable_value_key_")
        val DEBUG_LABEL_SUFFIX: String = "_DebugLabel"
        @TestOnly val DEBUG_CONTEXT_FOR_TESTS: Key<DebuggerContextImpl> = Key.create("DEBUG_CONTEXT_FOR_TESTS")

        fun getContextElement(elementAt: PsiElement?): PsiElement? {
            if (elementAt == null) return null

            if (elementAt is PsiCodeBlock) {
                return getContextElement(elementAt.context?.context)
            }

            if (elementAt is KtLightClass) {
                return getContextElement(elementAt.kotlinOrigin)
            }

            val containingFile = elementAt.containingFile
            if (containingFile is PsiJavaFile) return elementAt
            if (containingFile !is KtFile) return null

            // elementAt can be PsiWhiteSpace when codeFragment is created from line start offset (in case of first opening EE window)
            val lineStartOffset = if (elementAt is PsiWhiteSpace || elementAt is PsiComment) {
                PsiTreeUtil.skipSiblingsForward(elementAt, PsiWhiteSpace::class.java, PsiComment::class.java)?.textOffset ?: elementAt.textOffset
            } else {
                elementAt.textOffset
            }

            fun KtElement.takeIfAcceptedAsCodeFragmentContext() = takeIf { KotlinEditorTextProvider.isAcceptedAsCodeFragmentContext(it) }

            PsiTreeUtil.findElementOfClassAtOffset(containingFile, lineStartOffset, KtExpression::class.java, false)
                    ?.takeIfAcceptedAsCodeFragmentContext()
                    ?.let { return CodeInsightUtils.getTopmostElementAtOffset(it, lineStartOffset, KtExpression::class.java) }

            KotlinEditorTextProvider.findExpressionInner(elementAt, true)
                    ?.takeIfAcceptedAsCodeFragmentContext()
                    ?.let { return it }

            return containingFile
        }

        //internal for tests
        fun createCodeFragmentForLabeledObjects(project: Project, markupMap: Map<*, ValueMarkup>): Pair<String, Map<String, Value>> {
            @Suppress("UNCHECKED_CAST")
            val variables = markupMap.entries.associate {
                val (value, markup) = it
                "${markup.text}$DEBUG_LABEL_SUFFIX" to value as? Value
            }.filterValues { it != null } as Map<String, Value>

            return variables.kotlinVariablesAsText(project) to variables
        }

        private fun Map<String, Value>.kotlinVariablesAsText(project: Project): String {
            val sb = StringBuilder()

            val psiNameHelper = PsiNameHelper.getInstance(project)
            for ((variableName, variableValue) in entries) {
                if (!psiNameHelper.isIdentifier(variableName)) continue

                val variableTypeName = variableValue.type()?.name() ?: continue

                val kotlinProperty = createKotlinProperty(project, variableName, variableTypeName, variableValue) ?: continue

                sb.append("${kotlinProperty.text}\n")
            }

            sb.append("val _debug_context_val = 1\n")

            return sb.toString()
        }

        private fun createKotlinProperty(project: Project, variableName: String, variableTypeName: String, value: Value): KtProperty? {
            val actualClassDescriptor = value.asValue().asmType.getClassDescriptor(GlobalSearchScope.allScope(project))
            if (actualClassDescriptor != null && actualClassDescriptor.defaultType.arguments.isEmpty()) {
                val renderedType = IdeDescriptorRenderers.SOURCE_CODE.renderType(actualClassDescriptor.defaultType.makeNullable())
                return KtPsiFactory(project).createProperty(variableName.quoteIfNeeded(), renderedType, false)
            }

            fun String.addArraySuffix() = if (value is ArrayReference) this + "[]" else this

            val className = variableTypeName.replace("$", ".").substringBefore("[]")
            val classType = PsiType.getTypeByName(className, project, GlobalSearchScope.allScope(project))
            val type = (if (value !is PrimitiveValue && classType.resolve() == null)
                CommonClassNames.JAVA_LANG_OBJECT
            else
                className).addArraySuffix()

            val field = PsiElementFactory.SERVICE.getInstance(project).createField(variableName, PsiType.getTypeByName(type, project, GlobalSearchScope.allScope(project)))
            val ktField = field.j2k() as? KtProperty
            ktField?.modifierList?.delete()
            return ktField
        }
    }

    private fun wrapContextIfNeeded(project: Project, originalContext: PsiElement?, newContext: KtElement?): KtElement? {
        val markupMap: Map<*, ValueMarkup>? =
                if (ApplicationManager.getApplication().isUnitTestMode)
                    NodeDescriptorImpl.getMarkupMap(originalContext?.getCopyableUserData(DEBUG_CONTEXT_FOR_TESTS)?.debugProcess)
                else
                    (XDebuggerManager.getInstance(project).currentSession as? XDebugSessionImpl)?.valueMarkers?.allMarkers

        if (markupMap == null || markupMap.isEmpty()) return newContext

        val (text, labels) = createCodeFragmentForLabeledObjects(project, markupMap)
        if (text.isEmpty()) return newContext

        return createWrappingContext(text, labels, newContext, project)
    }

    private fun createFakeFileWithJavaContextElement(funWithLocalVariables: String, javaContext: PsiElement): KtFile {
        val javaFile = javaContext.containingFile as? PsiJavaFile

        val sb = StringBuilder()

        javaFile?.packageName?.takeUnless { it.isBlank() }?.let {
            sb.append("package ").append(it.quoteIfNeeded()).append("\n")
        }

        javaFile?.importList?.let { sb.append(it.text).append("\n") }

        sb.append(funWithLocalVariables)

        return KtPsiFactory(javaContext.project).createAnalyzableFile("fakeFileForJavaContextInDebugger.kt", sb.toString(), javaContext)
    }

    // internal for test
    fun createWrappingContext(
            newFragmentText: String,
            labels: Map<String, Value>,
            originalContext: KtElement?,
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

        return codeFragment.getContentElement().statements.lastOrNull()
    }
}
