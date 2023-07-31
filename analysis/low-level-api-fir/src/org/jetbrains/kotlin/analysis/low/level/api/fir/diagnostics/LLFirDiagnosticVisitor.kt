/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContextForProvider
import org.jetbrains.kotlin.fir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorComponents
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtDeclaration

internal open class LLFirDiagnosticVisitor(
    context: CheckerContextForProvider,
    components: DiagnosticCollectorComponents,
) : CheckerRunningDiagnosticCollectorVisitor(context, components) {
    private val beforeElementDiagnosticCollectionHandler = context.session.beforeElementDiagnosticCollectionHandler

    protected var useRegularComponents = true

    override fun visitNestedElements(element: FirElement) {
        if (element is FirDeclaration) {
            beforeElementDiagnosticCollectionHandler?.beforeGoingNestedDeclaration(element, context)
        }
        super.visitNestedElements(element)
    }

    override fun checkElement(element: FirElement) {
        if (useRegularComponents) {
            beforeElementDiagnosticCollectionHandler?.beforeCollectingForElement(element)
            components.regularComponents.forEach {
                checkCanceled()
                element.accept(it, context)
            }
        }
        checkCanceled()
        element.accept(components.reportCommitter, context)

        if (element is FirRegularClass) {
            suppressReportedDiagnosticsOnClassMembers(element)
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
            val module = ProjectStructureProvider.getModule(project, contextElement, contextualModule = null)
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
     * Some FirClassChecker may report diagnostics on class member headers.
     * That diagnostics should be suppressed if we have a `@Suppress` annotation on class member.
     */
    private fun suppressReportedDiagnosticsOnClassMembers(element: FirRegularClass) {
        for (member in element.declarations) {
            withAnnotationContainer(member) {
                member.accept(components.reportCommitter, context)
            }
        }
    }
}
