/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirSealedSupertypeChecker : FirClassChecker() {
    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        // only the file declaration is present
        if (declaration.classId.isLocal) {
            checkLocalDeclaration(declaration, context, reporter)
        } else {
            checkGlobalDeclaration(declaration, context, reporter)
        }
    }

    private fun checkGlobalDeclaration(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        for (it in declaration.superTypeRefs) {
            val classId = it.coneType.classId ?: continue

            if (classId.isLocal) {
                continue
            }

            val fir = context.session.symbolProvider.getClassLikeSymbolByFqName(classId)
                ?.fir.safeAs<FirRegularClass>()
                ?: continue

            if (fir.status.modality == Modality.SEALED && declaration.classId.packageFqName != fir.classId.packageFqName) {
                reporter.reportOn(it.source, FirErrors.SEALED_SUPERTYPE, context)
                continue
            }
        }
    }

    private fun checkLocalDeclaration(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        for (it in declaration.superTypeRefs) {
            val classId = it.coneType.classId ?: continue

            if (classId.isLocal) {
                continue
            }

            val fir = context.session.symbolProvider.getClassLikeSymbolByFqName(classId)
                ?.fir.safeAs<FirRegularClass>()
                ?: continue

            if (fir.status.modality == Modality.SEALED) {
                reporter.reportOn(it.source, FirErrors.SEALED_SUPERTYPE_IN_LOCAL_CLASS, context)
                return
            }
        }
    }
}
