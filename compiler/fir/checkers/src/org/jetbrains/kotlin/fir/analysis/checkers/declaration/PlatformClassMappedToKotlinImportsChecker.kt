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
import org.jetbrains.kotlin.fir.scopes.platformClassMapper
import org.jetbrains.kotlin.name.ClassId

object PlatformClassMappedToKotlinImportsChecker : FirFileChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        declaration.imports.forEach { import ->
            val importedFqName = import.importedFqName ?: return
            if (importedFqName.isRoot || importedFqName.shortName().asString().isEmpty()) return
            val classId = (import as? FirResolvedImport)?.resolvedParentClassId ?: ClassId.topLevel(importedFqName)
            if (classId.asSingleFqName() != importedFqName) {
                return
            }

            val kotlinClassId = context.session.platformClassMapper.getCorrespondingKotlinClass(classId)
            if (kotlinClassId != null) {
                reporter.reportOn(import.source, FirErrors.PLATFORM_CLASS_MAPPED_TO_KOTLIN, kotlinClassId)
            }
        }
    }
}