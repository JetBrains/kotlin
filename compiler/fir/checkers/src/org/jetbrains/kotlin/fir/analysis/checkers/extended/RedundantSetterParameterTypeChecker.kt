/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.fir.FirFakeSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirMemberDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_SETTER_PARAMETER_TYPE
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.toFirPsiSourceElement
import org.jetbrains.kotlin.psi.KtTypeReference

object RedundantSetterParameterTypeChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirProperty) return
        val setter = declaration.setter ?: return
        if (setter is FirDefaultPropertyAccessor) return
        val setterSource = setter.source ?: return
        if (setter.isParameterHasType) {
            val paramSource =
                setter.valueParameters.getOrNull(0)?.psi?.lastChild?.toFirPsiSourceElement() ?: setterSource
            reporter.report(paramSource, REDUNDANT_SETTER_PARAMETER_TYPE)
        }
    }

    private val FirPropertyAccessor.isParameterHasType
        get() = valueParameters.getOrNull(0)?.source?.psi?.children?.any { it is KtTypeReference } == true
                && source !is FirFakeSourceElement<*>
}