/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.openapi.diagnostic.thisLogger
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachDeclaration
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContextForProvider
import org.jetbrains.kotlin.fir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorComponents
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded

internal open class LLFirDiagnosticVisitor(
    context: CheckerContextForProvider,
    components: DiagnosticCollectorComponents,
) : CheckerRunningDiagnosticCollectorVisitor(context, components) {
    private val beforeElementDiagnosticCollectionHandler = context.session.beforeElementDiagnosticCollectionHandler

    override fun visitNestedElements(element: FirElement) {
        if (element is FirDeclaration) {
            beforeElementDiagnosticCollectionHandler?.beforeGoingNestedDeclaration(element, context)
        }

        super.visitNestedElements(element)
    }

    override fun checkElement(element: FirElement) {
        beforeElementDiagnosticCollectionHandler?.beforeCollectingForElement(element)
        components.regularComponents.forEach { diagnosticVisitor ->
            checkCanceled()
            suppressAndLogExceptions {
                element.accept(diagnosticVisitor, context)
            }
        }

        checkCanceled()
        suppressAndLogExceptions {
            element.accept(components.reportCommitter, context)
        }

        suppressAndLogExceptions {
            commitPendingDiagnosticsOnNestedDeclarations(element)
        }
    }

    override fun visitCodeFragment(codeFragment: FirCodeFragment, data: Nothing?) {
        val ktCodeFragment = codeFragment.psi as KtCodeFragment

        val contextElement = ktCodeFragment.context
        if (contextElement != null) {
            fun process(containingSymbols: List<FirDeclaration>) {
                if (containingSymbols.isEmpty()) {
                    super.visitCodeFragment(codeFragment, data)
                } else {
                    withDeclaration(containingSymbols.first()) {
                        process(containingSymbols.subList(1, containingSymbols.size))
                    }
                }
            }

            val project = contextElement.project
            val module = KotlinProjectStructureProvider.getModule(project, contextElement, useSiteModule = null)
            val resolveSession = module.getFirResolveSession(project)

            // Register containing declarations of a context element
            contextElement.parentsOfType<KtDeclaration>().toList().asReversed()
                .map { it.resolveToFirSymbol(resolveSession).fir }
                .run(::process)

            return
        }

        super.visitCodeFragment(codeFragment, data)
    }

    /**
     * File and class checkers may report diagnostics on top-level declarations and class members, such as conflicting overload errors.
     * Because we are collecting diagnostics for each structure element separately, this visitor will not visit these nested declarations by
     * default, as the file/class and its nested declarations are different structure elements. Instead, all diagnostics produced during the
     * visitor run will be committed at the end (see [FileStructureElementDiagnosticsCollector.collectForStructureElement]).
     *
     * Skipping nested declarations circumvents error suppression with `@Suppress` on top-level declarations and class members. This is
     * because suppression usually works as such: When a diagnostic is first reported on an element `E`, it is "pending". Once element `E`
     * is visited by the diagnostic visitor, it commits all pending diagnostics for `E`, including those reported by a file/class checker.
     * Diagnostics which are suppressed in the current context are instead removed. Without committing pending diagnostics on each element
     * `E`, suppression cannot take effect.
     *
     * [commitPendingDiagnosticsOnNestedDeclarations] commits pending diagnostics for directly nested elements, allowing the report
     * committer to take suppression into account.
     *
     * It suffices to commit pending diagnostics for directly nested declarations, because checkers can only report diagnostics on directly
     * accessible children. For example, a file checker can report a diagnostic on a top-level class, but not its member function.
     */
    private fun commitPendingDiagnosticsOnNestedDeclarations(element: FirElement) {
        val declarationContainer = when (element) {
            // Script `FirFile`s can be checked by file checkers, which report diagnostics on the declarations inside the `FirScript`, so we
            // have to unwrap the script from the file to commit the diagnostics on the script's declarations.
            is FirFile -> element.declarations.singleOrNull() as? FirScript ?: element

            is FirScript, is FirRegularClass -> element
            else -> return
        }

        // Casting to `FirDeclaration` is required in K1.
        @Suppress("USELESS_CAST")
        (declarationContainer as FirDeclaration).forEachDeclaration { declaration ->
            withAnnotationContainer(declaration) {
                declaration.accept(components.reportCommitter, context)
            }
        }
    }

    companion object {
        /**
         * We don't want to throw exceptions right away to not interrupt other checkers there possible.
         * It is better to report as much as possible and not crash the entire visitor.
         *
         * By default, a logger throws exceptions, but it is up to the user code to provide
         * an alternative handler.
         *
         * For instance, the IntelliJ plugin reports such exceptions and doesn't interrupt the execution flow.
         */
        inline fun <T> suppressAndLogExceptions(block: () -> T): T? = try {
            block()
        } catch (e: Throwable) {
            rethrowIntellijPlatformExceptionIfNeeded(e)

            thisLogger().error("The diagnostic collector has been interrupted by an exception. The result may be incomplete", e)
            null
        }
    }
}
