/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind.KSuspendFunction.SAFEST_MAX_ARITY as SAFEST_K_SUSPEND_MAX_ARITY
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirSyntheticFunctionInterfaceProviderBase.Companion.getArityIfAllowedOrNull
import org.jetbrains.kotlin.fir.types.functionTypeKind
import org.jetbrains.kotlin.name.ClassId

object FirTooLargeFunctionImportChecker : FirFileChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        for (import in declaration.imports) {
            val name = (import as? FirResolvedImport)?.importedName ?: continue
            val classId = ClassId(import.packageFqName, name)
            checkAndReportArityOf(classId, import.source)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun checkAndReportArityOf(classId: ClassId, source: KtSourceElement?) {
        val functionKind = classId.functionTypeKind(context.session) ?: return
        val declaredArity = classId.getArityIfAllowedOrNull(functionKind)

        when {
            declaredArity == null -> {
                reporter.reportOn(source, FirErrors.FUNCTION_TYPE_OF_TOO_LARGE_ARITY, classId, functionKind.maxArity)
            }
            functionKind == FunctionTypeKind.KSuspendFunction && declaredArity > SAFEST_K_SUSPEND_MAX_ARITY -> {
                reporter.reportOn(source, FirErrors.K_SUSPEND_FUNCTION_TYPE_OF_DANGEROUSLY_LARGE_ARITY, classId, SAFEST_K_SUSPEND_MAX_ARITY)
            }
        }
    }
}
