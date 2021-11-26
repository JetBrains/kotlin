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
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType

object FirSealedSupertypeChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        // only the file declaration is present
        if (declaration.classId.isLocal) {
            checkLocalDeclaration(declaration, context, reporter)
        } else {
            checkGlobalDeclaration(declaration, context, reporter)
        }
    }

    private fun checkGlobalDeclaration(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val subclassPackage = declaration.classId.packageFqName
        for (superTypeRef in declaration.superTypeRefs) {
            val superClassId = superTypeRef.coneType.classId ?: continue

            if (superClassId.isLocal) {
                continue
            }

            val superClass = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) as? FirRegularClassSymbol ?: continue

            if (!superClass.isSealed) continue
            if (superClass.origin == FirDeclarationOrigin.Java) {
                reporter.reportOn(superTypeRef.source, FirErrors.CLASS_INHERITS_JAVA_SEALED_CLASS, context)
                continue
            }
            val superClassPackage = superClass.classId.packageFqName
            if (superClassPackage != subclassPackage) {
                reporter.reportOn(superTypeRef.source, FirErrors.SEALED_INHERITOR_IN_DIFFERENT_PACKAGE, context)
            }
            if (superClass.moduleData != declaration.moduleData) {
                // TODO: implement logic like in org.jetbrains.kotlin.resolve.checkers.SealedInheritorInSameModuleChecker for MPP support.
                reporter.reportOn(superTypeRef.source, FirErrors.SEALED_INHERITOR_IN_DIFFERENT_MODULE, context)
            }
        }
    }

    private fun checkLocalDeclaration(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        for (it in declaration.superTypeRefs) {
            val classId = it.coneType.classId ?: continue

            if (classId.isLocal) {
                continue
            }

            val superClassSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: continue

            if (superClassSymbol.modality == Modality.SEALED) {
                val declarationType = if (declaration is FirAnonymousObject) "Anonymous object" else "Local class"
                reporter.reportOn(
                    it.source,
                    FirErrors.SEALED_SUPERTYPE_IN_LOCAL_CLASS,
                    declarationType,
                    superClassSymbol.classKind,
                    context
                )
                return
            }
        }
    }
}
