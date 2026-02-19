/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType

object FirSealedSupertypeChecker : FirClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        // only the file declaration is present
        if (declaration.isLocal) {
            checkLocalDeclaration(declaration)
        } else {
            checkGlobalDeclaration(declaration)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkGlobalDeclaration(declaration: FirClass) {
        val subclassPackage = declaration.classId.packageFqName
        for (superTypeRef in declaration.superTypeRefs) {
            val superClass = superTypeRef.coneType.fullyExpandedType().toRegularClassSymbol() ?: continue

            if (superClass.isLocal) {
                continue
            }

            if (!superClass.isSealed) continue
            if (superClass.origin is FirDeclarationOrigin.Java) {
                reporter.reportOn(superTypeRef.source, FirErrors.CLASS_INHERITS_JAVA_SEALED_CLASS)
                continue
            }
            val superClassPackage = superClass.classId.packageFqName
            if (superClassPackage != subclassPackage) {
                reporter.reportOn(superTypeRef.source, FirErrors.SEALED_INHERITOR_IN_DIFFERENT_PACKAGE)
            }
            if (superClass.moduleData != declaration.moduleData && !superClass.isExpect) {
                reporter.reportOn(superTypeRef.source, FirErrors.SEALED_INHERITOR_IN_DIFFERENT_MODULE)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkLocalDeclaration(declaration: FirClass) {
        for (it in declaration.superTypeRefs) {
            val superClassSymbol = it.coneType.fullyExpandedType().toRegularClassSymbol() ?: continue

            if (superClassSymbol.isLocal) {
                continue
            }

            if (superClassSymbol.modality == Modality.SEALED) {
                val declarationType = if (declaration is FirAnonymousObject) "Anonymous object" else "Local class"
                reporter.reportOn(
                    it.source,
                    FirErrors.SEALED_SUPERTYPE_IN_LOCAL_CLASS,
                    declarationType,
                    superClassSymbol.classKind
                )
                return
            }
        }
    }
}
