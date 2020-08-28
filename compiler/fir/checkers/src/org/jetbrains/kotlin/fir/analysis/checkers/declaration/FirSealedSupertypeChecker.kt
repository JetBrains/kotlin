/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirSealedSupertypeChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirClass<*>) {
            // only the file declaration is present
            when {
                context.containingDeclarations.size == 1 -> {
                    checkTopLevelDeclaration(declaration, context, reporter)
                }
                declaration.classId.isLocal -> {
                    checkLocalDeclaration(declaration, context, reporter)
                }
                else -> {
                    checkInnerDeclaration(declaration, context, reporter)
                }
            }
        } else if (declaration is FirProperty) {
            val initializer = declaration.initializer.safeAs<FirClass<*>>()
                ?: return

            checkLocalDeclaration(initializer, context, reporter)
        }
    }

    private fun checkTopLevelDeclaration(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        for (it in declaration.superTypeRefs) {
            val classId = it.safeAs<FirResolvedTypeRef>()
                ?.type.safeAs<ConeClassLikeType>()
                ?.lookupTag?.classId
                ?: continue

            if (classId.isLocal) {
                continue
            }

            val fir = context.session.firSymbolProvider.getClassLikeSymbolByFqName(classId)
                ?.fir.safeAs<FirRegularClass>()
                ?: continue

            if (fir.status.modality == Modality.SEALED && classId.outerClassId != null) {
                reporter.report(it.source)
                return
            }
        }
    }

    private fun checkLocalDeclaration(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        for (it in declaration.superTypeRefs) {
            val classId = it.safeAs<FirResolvedTypeRef>()
                ?.type.safeAs<ConeClassLikeType>()
                ?.lookupTag?.classId
                ?: continue

            if (classId.isLocal) {
                continue
            }

            val fir = context.session.firSymbolProvider.getClassLikeSymbolByFqName(classId)
                ?.fir.safeAs<FirRegularClass>()
                ?: continue

            if (fir.status.modality == Modality.SEALED) {
                reporter.reportInLocal(it.source)
                return
            }
        }
    }

    private fun checkInnerDeclaration(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        for (it in declaration.superTypeRefs) {
            val classId = it.safeAs<FirResolvedTypeRef>()
                ?.type.safeAs<ConeClassLikeType>()
                ?.lookupTag?.classId
                ?: continue

            if (classId.isLocal) {
                continue
            }

            val fir = context.session.firSymbolProvider.getClassLikeSymbolByFqName(classId)
                ?.fir.safeAs<FirRegularClass>()
                ?: continue

            if (fir.status.modality == Modality.SEALED && !context.containingDeclarations.contains(fir)) {
                reporter.report(it.source)
                return
            }
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.SEALED_SUPERTYPE.on(it)) }
    }

    private fun DiagnosticReporter.reportInLocal(source: FirSourceElement?) {
        source?.let { report(FirErrors.SEALED_SUPERTYPE_IN_LOCAL_CLASS.on(it)) }
    }
}