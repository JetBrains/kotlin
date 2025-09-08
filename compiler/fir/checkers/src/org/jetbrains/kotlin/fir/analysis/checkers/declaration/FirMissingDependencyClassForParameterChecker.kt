/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkMissingDependencySuperTypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirMissingDependencyClassProxy
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirMissingDependencyClassProxy.MissingTypeOrigin
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.typeAnnotations
import org.jetbrains.kotlin.name.Name

/**
 * In this checker, we check for two cases of missing dependency type:
 * - implicit type of lambda parameter
 * ```
 * foo { x -> ...}
 * ```
 * - explicit type of data class constructor parameter
 * ```
 * data class Foo(val x: Some)
 * ```
 * The second check is required, as we might call `x.toString/hashCode/equals` in the implictly generated functions of data class,
 * which wouldn't be checked by use-site checkers
 */
object FirMissingDependencyClassForParameterChecker : FirValueParameterChecker(MppCheckerKind.Common), FirMissingDependencyClassProxy {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirValueParameter) {
        val containingDeclaration = declaration.containingDeclarationSymbol

        when {
            containingDeclaration is FirAnonymousFunctionSymbol -> {
                checkLambdaParameter(declaration)
            }
            declaration.correspondingProperty != null && containingDeclaration.getContainingClassSymbol()?.isData == true -> {
                checkDataClassParameter(declaration)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkLambdaParameter(parameter: FirValueParameter) {
        if (parameter.returnTypeRef.source?.kind is KtRealSourceElementKind) return

        val missingTypes = mutableSetOf<ConeClassLikeType>()
        considerType(parameter.returnTypeRef.coneType, missingTypes)
        reportMissingTypes(
            parameter.source, missingTypes,
            missingTypeOrigin = MissingTypeOrigin.LambdaParameter(
                parameter.name.takeIf { !it.isSpecial } ?: Name.identifier("_")
            )
        )

        FirImplicitReturnTypeAnnotationMissingDependencyChecker.check(parameter.returnTypeRef, parameter.returnTypeRef.source)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDataClassParameter(parameter: FirValueParameter) {
        checkMissingDependencySuperTypes(parameter.returnTypeRef.coneType, parameter.source)
    }
}
