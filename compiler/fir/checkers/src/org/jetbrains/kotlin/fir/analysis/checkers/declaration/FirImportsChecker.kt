/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.name.ClassId

object FirImportsChecker : FirFileChecker() {
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.imports.forEach { import ->
            if (import is FirErrorImport) return@forEach
            if (import.isAllUnder && import !is FirResolvedImport) {
                checkAllUnderFromEnumEntry(import, context, reporter)
            }
            if (!import.isAllUnder) {
                checkCanBeImported(import, context, reporter)
            }
        }
    }

    private fun checkAllUnderFromEnumEntry(import: FirImport, context: CheckerContext, reporter: DiagnosticReporter) {
        val fqName = import.importedFqName ?: return
        if (fqName.isRoot || fqName.parent().isRoot) return
        val classId = ClassId.topLevel(fqName.parent())
        val classSymbol = context.session.symbolProvider.getClassLikeSymbolByFqName(classId) ?: return
        val classFir = classSymbol.fir as? FirRegularClass ?: return
        if (classFir.isEnumClass && classFir.collectEnumEntries().any { it.name == fqName.shortName() }) {
            reporter.reportOn(import.source, FirErrors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON, classFir.name, context)
        }
    }

    private fun checkCanBeImported(import: FirImport, context: CheckerContext, reporter: DiagnosticReporter) {
        val importedFqName = import.importedFqName ?: return
        val importedName = importedFqName.shortName()
        //empty name come from LT in some erroneous cases
        if (importedName.isSpecial || importedName.identifier.isEmpty()) return
        val classId = (import as? FirResolvedImport)?.resolvedClassId
        if (classId != null) {
            val classSymbol = context.session.symbolProvider.getClassLikeSymbolByFqName(classId) ?: return
            val classFir = classSymbol.fir as? FirRegularClass ?: return
            if (classFir.classKind.isSingleton) return
            
            val illegalImport = classFir.declarations.any {
                it is FirSimpleFunction && !it.isStatic && it.name == importedName ||
                        it is FirProperty && it.name == importedName
            }
            if (illegalImport) {
                reporter.reportOn(import.source, FirErrors.CANNOT_BE_IMPORTED, importedName, context)
            }
        } else {
            val importedClassId = ClassId.topLevel(importedFqName)
            if (context.session.symbolProvider.getClassLikeSymbolByFqName(importedClassId) != null) {
                return
            }
            context.session.symbolProvider.getPackage(importedFqName)?.let {
                reporter.reportOn(import.source, FirErrors.PACKAGE_CANNOT_BE_IMPORTED, context)
            }
        }
    }
}