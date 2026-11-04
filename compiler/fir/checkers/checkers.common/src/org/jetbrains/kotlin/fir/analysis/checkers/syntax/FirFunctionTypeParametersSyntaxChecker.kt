/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirNamedFunctionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds

object FirFunctionTypeParametersSyntaxChecker : FirNamedFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        val source = declaration.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        val typeParameterList = withSyntax { source.getChild(KotlinSyntaxNodeTypeIds.TYPE_PARAMETER_LIST, depth = 1) ?: return }
        val name = withSyntax { source.getChild(KotlinSyntaxTokenIds.IDENTIFIER, depth = 1) ?: return }

        if (typeParameterList.startOffset > name.startOffset) {
            reporter.reportOn(
                source,
                FirErrors.DEPRECATED_TYPE_PARAMETER_SYNTAX
            )
        }
    }
}
