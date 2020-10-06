/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory0
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*

object FirConstructorAllowedChecker : FirConstructorChecker() {
    override fun check(declaration: FirConstructor, context: CheckerContext, reporter: DiagnosticReporter) {
        val containingClass = context.containingDeclarations.lastOrNull() as? FirClass<*> ?: return
        val source = declaration.source
        val elementType = source?.elementType
        if (elementType != KtNodeTypes.PRIMARY_CONSTRUCTOR && elementType != KtNodeTypes.SECONDARY_CONSTRUCTOR) {
            return
        }
        when (containingClass.classKind) {
            ClassKind.OBJECT -> reporter.report(source, FirErrors.CONSTRUCTOR_IN_OBJECT)
            ClassKind.INTERFACE -> reporter.report(source, FirErrors.CONSTRUCTOR_IN_INTERFACE)
            ClassKind.ENUM_ENTRY -> reporter.report(source, FirErrors.CONSTRUCTOR_IN_OBJECT)
            ClassKind.ENUM_CLASS -> if (declaration.visibility != Visibilities.Private) {
                reporter.report(source, FirErrors.NON_PRIVATE_CONSTRUCTOR_IN_ENUM)
            }
            ClassKind.CLASS -> if (containingClass is FirRegularClass && containingClass.modality == Modality.SEALED &&
                declaration.visibility != Visibilities.Private
            ) {
                reporter.report(source, FirErrors.NON_PRIVATE_CONSTRUCTOR_IN_SEALED)
            }
            ClassKind.ANNOTATION_CLASS -> {
                // DO NOTHING
            }
        }
    }

    private inline fun <reified T : FirSourceElement, P : PsiElement> DiagnosticReporter.report(
        source: T?,
        factory: FirDiagnosticFactory0<T, P>
    ) {
        source?.let { report(factory.on(it)) }
    }
}
