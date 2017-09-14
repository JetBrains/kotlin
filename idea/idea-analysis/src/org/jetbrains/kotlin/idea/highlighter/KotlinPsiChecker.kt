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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightRangeExtension
import com.intellij.codeInsight.intention.EmptyIntentionAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.containers.MultiMap
import com.intellij.xml.util.XmlStringUtil
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.types.KotlinType
import java.lang.reflect.*
import java.util.*

open class KotlinPsiChecker : Annotator, HighlightRangeExtension {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile as? KtFile ?: return

        if (!KotlinHighlightingUtil.shouldHighlight(file)) return

        val analysisResult = file.analyzeFullyAndGetResult()
        if (analysisResult.isError()) {
            throw ProcessCanceledException(analysisResult.error)
        }

        val bindingContext = analysisResult.bindingContext

        getAfterAnalysisVisitor(holder, bindingContext).forEach { visitor -> element.accept(visitor) }

        annotateElement(element, holder, bindingContext.diagnostics)
    }

    override fun isForceHighlightParents(file: PsiFile): Boolean {
        return file is KtFile
    }

    open protected fun shouldSuppressUnusedParameter(parameter: KtParameter): Boolean = false

    fun annotateElement(element: PsiElement, holder: AnnotationHolder, diagnostics: Diagnostics) {
        val diagnosticsForElement = diagnostics.forElement(element)

        if (element is KtNameReferenceExpression) {
            val unresolved = diagnostics.any { it.factory == Errors.UNRESOLVED_REFERENCE }
            element.putUserData(UNRESOLVED_KEY, if (unresolved) Unit else null)
        }

        if (diagnosticsForElement.isEmpty()) return

        if (KotlinHighlightingUtil.shouldHighlightErrors(element)) {
            ElementAnnotator(element, holder, { param -> shouldSuppressUnusedParameter(param) }).registerDiagnosticsAnnotations(diagnosticsForElement)
        }
    }

    companion object {
        private fun getAfterAnalysisVisitor(holder: AnnotationHolder, bindingContext: BindingContext) = arrayOf(
                PropertiesHighlightingVisitor(holder, bindingContext),
                FunctionsHighlightingVisitor(holder, bindingContext),
                VariablesHighlightingVisitor(holder, bindingContext),
                TypeKindHighlightingVisitor(holder, bindingContext)
        )

        fun createQuickFixes(diagnostic: Diagnostic): Collection<IntentionAction> =
                createQuickFixes(listOfNotNull(diagnostic))[diagnostic]

        private val UNRESOLVED_KEY = Key<Unit>("KotlinPsiChecker.UNRESOLVED_KEY")

        fun wasUnresolved(element: KtNameReferenceExpression) = element.getUserData(UNRESOLVED_KEY) != null
    }
}

private fun createQuickFixes(similarDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction> {
    val first = similarDiagnostics.minBy { it.toString() }
    val factory = similarDiagnostics.first().factory

    val actions = MultiMap<Diagnostic, IntentionAction>()

    val intentionActionsFactories = QuickFixes.getInstance().getActionFactories(factory)
    for (intentionActionsFactory in intentionActionsFactories) {
        val allProblemsActions = intentionActionsFactory.createActionsForAllProblems(similarDiagnostics)
        if (!allProblemsActions.isEmpty()) {
            actions.putValues(first, allProblemsActions)
        }
        else {
            for (diagnostic in similarDiagnostics) {
                actions.putValues(diagnostic, intentionActionsFactory.createActions(diagnostic))
            }
        }
    }

    for (diagnostic in similarDiagnostics) {
        actions.putValues(diagnostic, QuickFixes.getInstance().getActions(diagnostic.factory))
    }

    actions.values().forEach { NoDeclarationDescriptorsChecker.check(it::class.java) }

    return actions
}

private object NoDeclarationDescriptorsChecker {
    private val LOG = Logger.getInstance(NoDeclarationDescriptorsChecker::class.java)

    private val checkedQuickFixClasses = Collections.synchronizedSet(HashSet<Class<*>>())

    fun check(quickFixClass: Class<*>) {
        if (!checkedQuickFixClasses.add(quickFixClass)) return

        for (field in quickFixClass.declaredFields) {
            checkType(field.genericType, field)
        }

        quickFixClass.superclass?.let { check(it) }
    }

    private fun checkType(type: Type, field: Field) {
        when (type) {
            is Class<*> -> {
                if (DeclarationDescriptor::class.java.isAssignableFrom(type) || KotlinType::class.java.isAssignableFrom(type)) {
                    LOG.error("QuickFix class ${field.declaringClass.name} contains field ${field.name} that holds ${type.simpleName}. "
                              + "This leads to holding too much memory through this quick-fix instance. "
                              + "Possible solution can be wrapping it using KotlinIntentionActionFactoryWithDelegate.")
                }

                if (IntentionAction::class.java.isAssignableFrom(type)) {
                    check(type)
                }

            }

            is GenericArrayType -> checkType(type.genericComponentType, field)

            is ParameterizedType -> {
                if (Collection::class.java.isAssignableFrom(type.rawType as Class<*>)) {
                    type.actualTypeArguments.forEach { checkType(it, field) }
                }
            }

            is WildcardType -> type.upperBounds.forEach { checkType(it, field) }
        }
    }
}

private class ElementAnnotator(private val element: PsiElement,
                               private val holder: AnnotationHolder,
                               private val shouldSuppressUnusedParameter: (KtParameter) -> Boolean) {
    fun registerDiagnosticsAnnotations(diagnostics: Collection<Diagnostic>) {
        diagnostics.groupBy { it.factory }.forEach { group -> registerDiagnosticAnnotations(group.value) }
    }

    private fun registerDiagnosticAnnotations(diagnostics: List<Diagnostic>) {
        assert(diagnostics.isNotEmpty())

        val validDiagnostics = diagnostics.filter { it.isValid }
        if (validDiagnostics.isEmpty()) return

        val diagnostic = diagnostics.first()
        val factory = diagnostic.factory

        assert(diagnostics.all { it.psiElement == element && it.factory == factory })

        val ranges = diagnostic.textRanges

        val presentationInfo: AnnotationPresentationInfo = when (factory.severity) {
            Severity.ERROR -> {
                when (factory) {
                    in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS -> {
                        val referenceExpression = element as KtReferenceExpression
                        val reference = referenceExpression.mainReference
                        if (reference is MultiRangeReference) {
                            AnnotationPresentationInfo(
                                    ranges = reference.ranges.map { it.shiftRight(referenceExpression.textOffset) },
                                    highlightType = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        }
                        else {
                            AnnotationPresentationInfo(ranges, highlightType = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        }
                    }

                    Errors.ILLEGAL_ESCAPE -> AnnotationPresentationInfo(ranges, textAttributes = KotlinHighlightingColors.INVALID_STRING_ESCAPE)

                    Errors.REDECLARATION -> AnnotationPresentationInfo(
                            ranges = listOf(diagnostic.textRanges.first()), nonDefaultMessage = "")

                    else -> {
                        AnnotationPresentationInfo(
                                ranges,
                                highlightType = if (factory == Errors.INVISIBLE_REFERENCE)
                                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                                else
                                    null)
                    }
                }
            }
            Severity.WARNING -> {
                if (factory == Errors.UNUSED_PARAMETER && shouldSuppressUnusedParameter(element as KtParameter)) {
                    return
                }

                AnnotationPresentationInfo(
                        ranges,
                        textAttributes = when (factory) {
                            Errors.DEPRECATION -> CodeInsightColors.DEPRECATED_ATTRIBUTES
                            Errors.UNUSED_ANONYMOUS_PARAMETER -> CodeInsightColors.WEAK_WARNING_ATTRIBUTES
                            else -> null
                        },
                        highlightType = when (factory) {
                            in Errors.UNUSED_ELEMENT_DIAGNOSTICS -> ProblemHighlightType.LIKE_UNUSED_SYMBOL
                            Errors.UNUSED_ANONYMOUS_PARAMETER -> ProblemHighlightType.WEAK_WARNING
                            else -> null
                        }
                )
            }
            Severity.INFO -> AnnotationPresentationInfo(ranges, highlightType = ProblemHighlightType.INFORMATION)
        }

        setUpAnnotations(diagnostics, presentationInfo)
    }

    private fun setUpAnnotations(diagnostics: List<Diagnostic>, data: AnnotationPresentationInfo) {
        val fixesMap = try {
            createQuickFixes(diagnostics)
        }
        catch (e: Exception) {
            if (e is ControlFlowException) {
                throw e
            }
            LOG.error(e)
            MultiMap<Diagnostic, IntentionAction>()
        }

        for (range in data.ranges) {
            for (diagnostic in diagnostics) {
                val annotation = data.create(diagnostic, range, holder)
                val fixes = fixesMap[diagnostic]

                fixes.forEach { annotation.registerFix(it) }

                if (diagnostic.severity == Severity.WARNING) {
                    annotation.problemGroup = KotlinSuppressableWarningProblemGroup(diagnostic.factory)

                    if (fixes.isEmpty()) {
                        // if there are no quick fixes we need to register an EmptyIntentionAction to enable 'suppress' actions
                        annotation.registerFix(EmptyIntentionAction(diagnostic.factory.name))
                    }
                }
            }
        }
    }

    companion object {
        val LOG = Logger.getInstance(ElementAnnotator::class.java)
    }
}

private class AnnotationPresentationInfo(
        val ranges: List<TextRange>,
        val nonDefaultMessage: String? = null,
        val highlightType: ProblemHighlightType? = null,
        val textAttributes: TextAttributesKey? = null
) {

    fun create(diagnostic: Diagnostic, range: TextRange, holder: AnnotationHolder): Annotation {
        val defaultMessage = nonDefaultMessage ?: getDefaultMessage(diagnostic)

        val annotation = when (diagnostic.severity) {
            Severity.ERROR -> holder.createErrorAnnotation(range, defaultMessage)
            Severity.WARNING -> {
                if (highlightType == ProblemHighlightType.WEAK_WARNING) {
                    holder.createWeakWarningAnnotation(range, defaultMessage)
                }
                else {
                    holder.createWarningAnnotation(range, defaultMessage)
                }
            }
            Severity.INFO -> holder.createInfoAnnotation(range, defaultMessage)
        }

        annotation.tooltip = getMessage(diagnostic)

        if (highlightType != null) {
            annotation.highlightType = highlightType
        }

        if (textAttributes != null) {
            annotation.textAttributes = textAttributes
        }

        return annotation
    }

    private fun getMessage(diagnostic: Diagnostic): String {
        var message = IdeErrorMessages.render(diagnostic)
        if (KotlinInternalMode.enabled || ApplicationManager.getApplication().isUnitTestMode) {
            val factoryName = diagnostic.factory.name
            message = if (message.startsWith("<html>")) {
                "<html>[$factoryName] ${message.substring("<html>".length)}"
            }
            else {
                "[$factoryName] $message"
            }
        }
        if (!message.startsWith("<html>")) {
            message = "<html><body>${XmlStringUtil.escapeString(message)}</body></html>"
        }
        return message
    }

    private fun getDefaultMessage(diagnostic: Diagnostic): String {
        val message = DefaultErrorMessages.render(diagnostic)
        if (KotlinInternalMode.enabled || ApplicationManager.getApplication().isUnitTestMode) {
            return "[${diagnostic.factory.name}] $message"
        }
        return message
    }
}
