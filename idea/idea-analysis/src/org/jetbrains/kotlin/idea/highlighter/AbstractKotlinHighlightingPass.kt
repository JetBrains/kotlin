/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.Divider
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.CommonProcessors
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import java.lang.reflect.*
import java.util.*

abstract class AbstractKotlinHighlightingPass(file: KtFile, document: Document) :
    AbstractBindingContextAwareHighlightingPassBase(file, document) {
    override val annotator: Annotator
        get() = KotlinAfterAnalysisAnnotator()

    private inner class KotlinAfterAnalysisAnnotator : Annotator {
        override fun annotate(element: PsiElement, holder: AnnotationHolder) {
            val bindingContext = bindingContext()
            getAfterAnalysisVisitor(holder, bindingContext).forEach { visitor -> element.accept(visitor) }
        }
    }

    override fun buildBindingContext(holder: AnnotationHolder): BindingContext {
        val dividedElements: List<Divider.DividedElements> = ArrayList()
        Divider.divideInsideAndOutsideAllRoots(
            file, file.textRange, file.textRange, { true },
            CommonProcessors.CollectProcessor(dividedElements)
        )
        // TODO: for the sake of check that element belongs to the file
        //  for some reason analyzeWithAllCompilerChecks could return psiElements those do not belong to the file
        //  see [ScriptConfigurationHighlightingTestGenerated$Highlighting.testCustomExtension]
        val elements = dividedElements.flatMap(Divider.DividedElements::inside).toSet()

        // annotate diagnostics on fly: show diagnostics as soon as front-end reports them
        // don't create quick fixes as it could require some resolve
        val annotationByDiagnostic = mutableMapOf<Diagnostic, Annotation>()
        val annotationByTextRange = mutableMapOf<TextRange, Annotation>()

        // render of on-fly diagnostics with descriptors could lead to recursion
        fun checkIfDescriptor(candidate: Any?): Boolean =
            candidate is DeclarationDescriptor || candidate is Collection<*> && candidate.any(::checkIfDescriptor)

        val analysisResult =
            file.analyzeWithAllCompilerChecks({
                                                  val element = it.psiElement
                                                  if (element in elements &&
                                                      it !in annotationByDiagnostic &&
                                                      !RenderingContext.parameters(it).any(::checkIfDescriptor)
                                                  ) {
                                                      annotateDiagnostic(element, holder, it, annotationByDiagnostic, annotationByTextRange)
                                                  }
                                              }).also { it.throwIfError() }
        // resolve is done!

        val bindingContext = analysisResult.bindingContext

        cleanUpCalculatingAnnotations(annotationByTextRange)
        // TODO: for some reasons it could be duplicated diagnostics for the same factory
        //  see [PsiCheckerTestGenerated$Checker.testRedeclaration]
        val diagnostics = bindingContext.diagnostics.asSequence().filter { it.psiElement in elements }.toSet()

        if (diagnostics.isNotEmpty()) {
            // annotate diagnostics those were not possible to render on fly
            diagnostics.asSequence().filterNot { it in annotationByDiagnostic }.forEach {
                annotateDiagnostic(it.psiElement, holder, it, annotationByDiagnostic, calculatingInProgress = false)
            }
            // apply quick fixes for all diagnostics grouping by element
            diagnostics.groupBy(Diagnostic::psiElement).forEach {
                annotateQuickFixes(it.key, it.value, annotationByDiagnostic)
            }
        }
        return bindingContext
    }

    private fun annotateDiagnostic(
        element: PsiElement,
        holder: AnnotationHolder,
        diagnostic: Diagnostic,
        annotationByDiagnostic: MutableMap<Diagnostic, Annotation>? = null,
        annotationByTextRange: MutableMap<TextRange, Annotation>? = null,
        calculatingInProgress: Boolean = true
    ) = annotateDiagnostics(element, holder, listOf(diagnostic), annotationByDiagnostic, annotationByTextRange, true, calculatingInProgress)

    private fun cleanUpCalculatingAnnotations(annotationByTextRange: Map<TextRange, Annotation>) {
        annotationByTextRange.values.forEach { annotation ->
            annotation.quickFixes?.removeIf {
                it.quickFix is CalculatingIntentionAction
            }
        }
    }

    private fun annotateDiagnostics(
        element: PsiElement,
        holder: AnnotationHolder,
        diagnostics: List<Diagnostic>,
        annotationByDiagnostic: MutableMap<Diagnostic, Annotation>? = null,
        annotationByTextRange: MutableMap<TextRange, Annotation>? = null,
        noFixes: Boolean = false,
        calculatingInProgress: Boolean = false
    ) = annotateDiagnostics(
        file, element, holder, diagnostics, annotationByDiagnostic, annotationByTextRange,
        ::shouldSuppressUnusedParameter,
        noFixes = noFixes, calculatingInProgress = calculatingInProgress
    )

    /**
     * [diagnostics] has to belong to the same element
     */
    private fun annotateQuickFixes(
        element: PsiElement,
        diagnostics: List<Diagnostic>,
        annotationByDiagnostic: MutableMap<Diagnostic, Annotation>
    ) {
        if (diagnostics.isEmpty()) return

        assertBelongsToTheSameElement(element, diagnostics)

        val shouldHighlightErrors =
            KotlinHighlightingUtil.shouldHighlightErrors(
                if (element.isPhysical) file else element
            )

        if (shouldHighlightErrors) {
            ElementAnnotator(element) { param ->
                shouldSuppressUnusedParameter(param)
            }.registerDiagnosticsQuickFixes(diagnostics, annotationByDiagnostic)
        }
    }

    protected open fun shouldSuppressUnusedParameter(parameter: KtParameter): Boolean = false

    companion object {
        fun createQuickFixes(diagnostic: Diagnostic): Collection<IntentionAction> =
            createQuickFixes(listOfNotNull(diagnostic))[diagnostic]

        private val UNRESOLVED_KEY = Key<Unit>("KotlinHighlightingPass.UNRESOLVED_KEY")

        fun wasUnresolved(element: KtNameReferenceExpression) = element.getUserData(UNRESOLVED_KEY) != null

        fun getAfterAnalysisVisitor(holder: AnnotationHolder, bindingContext: BindingContext) = arrayOf(
            PropertiesHighlightingVisitor(holder, bindingContext),
            FunctionsHighlightingVisitor(holder, bindingContext),
            VariablesHighlightingVisitor(holder, bindingContext),
            TypeKindHighlightingVisitor(holder, bindingContext)
        )

        private fun assertBelongsToTheSameElement(element: PsiElement, diagnostics: Collection<Diagnostic>) {
            assert(diagnostics.all { it.psiElement == element })
        }

        fun annotateDiagnostics(
            file: KtFile,
            element: PsiElement,
            holder: AnnotationHolder,
            diagnostics: Collection<Diagnostic>,
            annotationByDiagnostic: MutableMap<Diagnostic, Annotation>? = null,
            annotationByTextRange: MutableMap<TextRange, Annotation>? = null,
            shouldSuppressUnusedParameter: (KtParameter) -> Boolean = { false },
            noFixes: Boolean = false,
            calculatingInProgress: Boolean = false
        ) {
            if (diagnostics.isEmpty()) return

            assertBelongsToTheSameElement(element, diagnostics)

            if (element is KtNameReferenceExpression) {
                val unresolved = diagnostics.any { it.factory == Errors.UNRESOLVED_REFERENCE }
                element.putUserData(UNRESOLVED_KEY, if (unresolved) Unit else null)
            }

            val shouldHighlightErrors =
                KotlinHighlightingUtil.shouldHighlightErrors(
                    if (element.isPhysical) file else element
                )

            if (shouldHighlightErrors) {
                val elementAnnotator = ElementAnnotator(element) { param ->
                    shouldSuppressUnusedParameter(param)
                }
                elementAnnotator.registerDiagnosticsAnnotations(
                    holder, diagnostics, annotationByDiagnostic,
                    annotationByTextRange,
                    noFixes = noFixes, calculatingInProgress = calculatingInProgress
                )
            }
        }
    }
}


internal fun createQuickFixes(similarDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction> {
    val first = similarDiagnostics.minByOrNull { it.toString() }
    val factory = similarDiagnostics.first().getRealDiagnosticFactory()

    val actions = MultiMap<Diagnostic, IntentionAction>()

    val intentionActionsFactories = QuickFixes.getInstance().getActionFactories(factory)
    for (intentionActionsFactory in intentionActionsFactories) {
        val allProblemsActions = intentionActionsFactory.createActionsForAllProblems(similarDiagnostics)
        if (allProblemsActions.isNotEmpty()) {
            actions.putValues(first, allProblemsActions)
        } else {
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

private fun Diagnostic.getRealDiagnosticFactory(): DiagnosticFactory<*> =
    when (factory) {
        Errors.PLUGIN_ERROR -> Errors.PLUGIN_ERROR.cast(this).a.factory
        Errors.PLUGIN_WARNING -> Errors.PLUGIN_WARNING.cast(this).a.factory
        Errors.PLUGIN_INFO -> Errors.PLUGIN_INFO.cast(this).a.factory
        else -> factory
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
                    LOG.error(
                        "QuickFix class ${field.declaringClass.name} contains field ${field.name} that holds ${type.simpleName}. "
                                + "This leads to holding too much memory through this quick-fix instance. "
                                + "Possible solution can be wrapping it using KotlinIntentionActionFactoryWithDelegate."
                    )
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
