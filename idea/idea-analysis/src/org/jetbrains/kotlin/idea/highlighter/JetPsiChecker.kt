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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightRangeExtension
import com.intellij.codeInsight.intention.EmptyIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.xml.util.XmlStringUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import kotlin.platform.platformStatic
import org.jetbrains.kotlin.idea.kdoc.KDocHighlightingVisitor
import org.jetbrains.kotlin.psi.JetParameter

public open class JetPsiChecker : Annotator, HighlightRangeExtension {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!(ProjectRootsUtil.isInProjectOrLibraryContent(element) || element.getContainingFile() is JetCodeFragment)) return

        getBeforeAnalysisVisitors(holder).forEach { visitor -> element.accept(visitor) }

        val file = element.getContainingFile() as JetFile

        val analysisResult = file.analyzeFullyAndGetResult()
        if (analysisResult.isError()) {
            throw ProcessCanceledException(analysisResult.error)
        }

        val bindingContext = analysisResult.bindingContext

        getAfterAnalysisVisitor(holder, bindingContext).forEach { visitor -> element.accept(visitor) }

        annotateElement(element, holder, bindingContext.getDiagnostics())
    }

    override fun isForceHighlightParents(file: PsiFile): Boolean {
        return file is JetFile
    }

    open protected fun shouldSuppressUnusedParameter(parameter: JetParameter): Boolean = false

    fun annotateElement(element: PsiElement, holder: AnnotationHolder, diagnostics: Diagnostics) {
        if (ProjectRootsUtil.isInProjectSource(element) || element.getContainingFile() is JetCodeFragment) {
            val elementAnnotator = ElementAnnotator(element, holder)
            for (diagnostic in diagnostics.forElement(element)) {
                elementAnnotator.registerDiagnosticAnnotations(diagnostic)
            }
        }
    }

    private inner class ElementAnnotator(private val element: PsiElement, private val holder: AnnotationHolder) {
        private var isMarkedWithRedeclaration = false

        fun registerDiagnosticAnnotations(diagnostic: Diagnostic) {
            if (!diagnostic.isValid()) return

            assert(diagnostic.getPsiElement() == element)

            val textRanges = diagnostic.getTextRanges()
            val factory = diagnostic.getFactory()
            when (diagnostic.getSeverity()) {
                Severity.ERROR -> {

                    when (factory) {
                        in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS -> {
                            val referenceExpression = diagnostic.getPsiElement() as JetReferenceExpression
                            val reference = referenceExpression.getReference()
                            if (reference is MultiRangeReference) {
                                for (range in reference.getRanges()) {
                                    val annotation = holder.createErrorAnnotation(range.shiftRight(referenceExpression.getTextOffset()), getDefaultMessage(diagnostic))
                                    setUpAnnotation(diagnostic, annotation, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                                }
                            }
                            else {
                                for (textRange in textRanges) {
                                    val annotation = holder.createErrorAnnotation(textRange, getDefaultMessage(diagnostic))
                                    setUpAnnotation(diagnostic, annotation, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                                }
                            }
                        }

                        Errors.ILLEGAL_ESCAPE -> {
                            for (textRange in textRanges) {
                                val annotation = holder.createErrorAnnotation(textRange, getDefaultMessage(diagnostic))
                                annotation.setTooltip(getMessage(diagnostic))
                                annotation.setTextAttributes(JetHighlightingColors.INVALID_STRING_ESCAPE)
                            }
                        }

                        Errors.REDECLARATION -> {
                            if (!isMarkedWithRedeclaration) {
                                isMarkedWithRedeclaration = true
                                val annotation = holder.createErrorAnnotation(diagnostic.getTextRanges()[0], "")
                                setUpAnnotation(diagnostic, annotation, null)
                            }
                        }

                        else -> {
                            for (textRange in textRanges) {
                                val annotation = holder.createErrorAnnotation(textRange, getDefaultMessage(diagnostic))
                                setUpAnnotation(diagnostic, annotation, if (factory == Errors.INVISIBLE_REFERENCE)
                                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                                else
                                    null)
                            }
                        }
                    }
                }
                Severity.WARNING -> {
                    if (factory == Errors.UNUSED_PARAMETER && shouldSuppressUnusedParameter(diagnostic.getPsiElement() as JetParameter)) {
                        return
                    }
                    for (textRange in textRanges) {
                        val annotation = holder.createWarningAnnotation(textRange, getDefaultMessage(diagnostic))

                        if (factory == Errors.DEPRECATED_CLASS_OBJECT_SYNTAX) annotation.setTextAttributes(CodeInsightColors.DEPRECATED_ATTRIBUTES)

                        setUpAnnotation(diagnostic, annotation, if (factory in Errors.UNUSED_ELEMENT_DIAGNOSTICS)
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL
                        else
                            null)
                    }
                }
            }
        }

        private fun setUpAnnotation(diagnostic: Diagnostic, annotation: Annotation, highlightType: ProblemHighlightType?) {
            annotation.setTooltip(getMessage(diagnostic))
            registerQuickFix(annotation, diagnostic)

            if (highlightType != null) {
                annotation.setHighlightType(highlightType)
            }
        }

        private fun registerQuickFix(annotation: Annotation, diagnostic: Diagnostic) {
            val intentionActionsFactories = QuickFixes.getActionsFactories(diagnostic.getFactory())
            for (intentionActionsFactory in intentionActionsFactories) {
                if (intentionActionsFactory != null) {
                    for (action in intentionActionsFactory.createActions(diagnostic)) {
                        annotation.registerFix(action)
                    }
                }
            }

            val actions = QuickFixes.getActions(diagnostic.getFactory())
            for (action in actions) {
                annotation.registerFix(action)
            }

            // Making warnings suppressable
            if (diagnostic.getSeverity() == Severity.WARNING) {
                annotation.setProblemGroup(KotlinSuppressableWarningProblemGroup(diagnostic.getFactory()))

                val fixes = annotation.getQuickFixes()
                if (fixes == null || fixes.isEmpty()) {
                    // if there are no quick fixes we need to register an EmptyIntentionAction to enable 'suppress' actions
                    annotation.registerFix(EmptyIntentionAction(diagnostic.getFactory().getName()))
                }
            }
        }

        private fun getMessage(diagnostic: Diagnostic): String {
            var message = IdeErrorMessages.render(diagnostic)
            if (KotlinInternalMode.enabled || ApplicationManager.getApplication().isUnitTestMode()) {
                val factoryName = diagnostic.getFactory().getName()
                if (message.startsWith("<html>")) {
                    message = "<html>[$factoryName] ${message.substring("<html>".length())}"
                }
                else {
                    message = "[$factoryName] $message"
                }
            }
            if (!message.startsWith("<html>")) {
                message = "<html><body>${XmlStringUtil.escapeString(message)}</body></html>"
            }
            return message
        }

        private fun getDefaultMessage(diagnostic: Diagnostic): String {
            val message = DefaultErrorMessages.render(diagnostic)
            if (KotlinInternalMode.enabled || ApplicationManager.getApplication().isUnitTestMode()) {
                return "[${diagnostic.getFactory().getName()}] $message"
            }
            return message
        }
    }

    default object {
        var namesHighlightingEnabled = true
            [TestOnly] set

        platformStatic fun highlightName(holder: AnnotationHolder, psiElement: PsiElement, attributesKey: TextAttributesKey) {
            if (namesHighlightingEnabled) {
                holder.createInfoAnnotation(psiElement, null).setTextAttributes(attributesKey)
            }
        }

        private fun getBeforeAnalysisVisitors(holder: AnnotationHolder) = array(
                SoftKeywordsHighlightingVisitor(holder),
                LabelsHighlightingVisitor(holder),
                KDocHighlightingVisitor(holder)
        )

        private fun getAfterAnalysisVisitor(holder: AnnotationHolder, bindingContext: BindingContext) = array(
                PropertiesHighlightingVisitor(holder, bindingContext),
                FunctionsHighlightingVisitor(holder, bindingContext),
                VariablesHighlightingVisitor(holder, bindingContext),
                TypeKindHighlightingVisitor(holder, bindingContext),
                DeprecatedAnnotationVisitor(holder, bindingContext)
        )

    }
}
