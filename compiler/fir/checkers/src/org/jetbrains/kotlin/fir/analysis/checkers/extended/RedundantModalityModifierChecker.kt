/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirFakeSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirMemberDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.implicitModality
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_MODALITY_MODIFIER
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.modality
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.toFirPsiSourceElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier

object RedundantModalityModifierChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.source is FirFakeSourceElement<*>) return

        val modality = declaration.modality ?: return
        if (
            modality == Modality.FINAL
            && (context.containingDeclarations.last() as? FirClass<*>)?.classKind == ClassKind.INTERFACE
        ) return

        val implicitModality = declaration.implicitModality(context)

        if (modality != implicitModality) return

        val modalityModifier = (declaration.psi as? KtDeclaration)?.modalityModifier() ?: return
        reporter.report(modalityModifier.toFirPsiSourceElement(), REDUNDANT_MODALITY_MODIFIER)
    }
}
