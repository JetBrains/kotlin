/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

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
            val functionKind = classId.functionTypeKind(context.session) ?: continue
            val declaredArity = classId.getArityIfAllowedOrNull(functionKind)

            if (declaredArity == null) {
                reporter.reportOn(import.source, FirErrors.FUNCTION_TYPE_OF_TOO_LARGE_ARITY, classId, functionKind.maxArity)
            }
        }
    }
}
