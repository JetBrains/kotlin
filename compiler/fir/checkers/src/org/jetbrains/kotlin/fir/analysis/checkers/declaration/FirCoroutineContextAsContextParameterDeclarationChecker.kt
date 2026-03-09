/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.StandardClassIds

object FirCoroutineContextAsContextParameterDeclarationChecker : FirCallableDeclarationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirCallableDeclaration) {
        for (parameter in declaration.contextParameters) {
            val type = parameter.returnTypeRef.coneType
            if (type.classId == StandardClassIds.CoroutineContext) {
                reporter.reportOn(parameter.source, FirErrors.COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED)
            }
        }
    }
}

object FirCoroutineContextAsContextParameterTypeRefChecker : FirTypeRefChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirTypeRef) {
        if (typeRef is FirFunctionTypeRef && typeRef.contextParameterTypeRefs.any { it.coneType.classId == StandardClassIds.CoroutineContext }) {
            reporter.reportOn(typeRef.source, FirErrors.COROUTINE_CONTEXT_AS_CONTEXT_PARAMETER_IS_RESERVED)
        }
    }
}
