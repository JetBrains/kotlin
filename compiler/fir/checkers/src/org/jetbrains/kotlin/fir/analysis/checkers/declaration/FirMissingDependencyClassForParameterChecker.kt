/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirMissingDependencyClassProxy
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name

object FirMissingDependencyClassForParameterChecker : FirValueParameterChecker(MppCheckerKind.Common), FirMissingDependencyClassProxy {
    override fun check(
        declaration: FirValueParameter,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val containingFunctionSymbol = declaration.containingDeclarationSymbol
        if (containingFunctionSymbol !is FirAnonymousFunctionSymbol) return
        if (declaration.returnTypeRef.source?.kind is KtRealSourceElementKind) return

        val missingTypes = mutableSetOf<ConeKotlinType>()
        considerType(declaration.returnTypeRef.coneType, missingTypes, context)
        reportMissingTypes(
            declaration.source, missingTypes, context, reporter,
            missingTypeOrigin = FirMissingDependencyClassProxy.MissingTypeOrigin.LambdaParameter(
                declaration.name.takeIf { !it.isSpecial } ?: Name.identifier("_")
            )
        )
    }
}
