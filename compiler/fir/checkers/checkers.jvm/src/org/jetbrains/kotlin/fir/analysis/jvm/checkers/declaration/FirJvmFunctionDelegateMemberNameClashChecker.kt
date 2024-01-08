/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isFun
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.Name

object FirJvmFunctionDelegateMemberNameClashChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    private val functionDelegateName: Name = Name.identifier("functionDelegate")
    private val getFunctionDelegateName: Name = Name.identifier("getFunctionDelegate")

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirCallableDeclaration) return
        val containingClassSymbol = declaration.getContainingClassSymbol(context.session) as? FirRegularClassSymbol ?: return
        if (!containingClassSymbol.isFun) return
        if (declaration.symbol.isExtension || (declaration as? FirFunction)?.valueParameters?.isNotEmpty() == true) return

        if (declaration is FirSimpleFunction && declaration.name == getFunctionDelegateName ||
            declaration is FirProperty && declaration.name == functionDelegateName
        ) {
            reporter.reportOn(declaration.source, FirJvmErrors.FUNCTION_DELEGATE_MEMBER_NAME_CLASH, context)
        }
    }
}
