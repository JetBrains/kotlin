/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds

object FirLocalVariableTypeParametersSyntaxChecker : FirDeclarationSyntaxChecker<FirProperty>() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(
        element: FirProperty,
        source: KtSourceElement,
    ) {
        if (source.kind is KtFakeSourceElementKind || element.symbol !is FirLocalPropertySymbol) return
        // Local variables are also used in increments/decrements of dot-qualified expressions, whose source isn't a property.
        if (source.syntaxId != KotlinSyntaxNodeTypeIds.PROPERTY) return

        if (withSyntax { source.getChild(KotlinSyntaxNodeTypeIds.TYPE_PARAMETER_LIST, depth = 1) } != null) {
            reporter.reportOn(source, FirErrors.LOCAL_VARIABLE_WITH_TYPE_PARAMETERS)
        }
    }
}
