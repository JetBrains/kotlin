/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.parentOrNull

internal object FirPackageConflictsWithClassifierChecker : FirFileChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        val packageDirective = declaration.packageDirective
        val filePackageFqName = packageDirective.packageFqName
        for (currentPackageFqName in filePackageFqName.parentsIncludingSelf()) {
            if (currentPackageFqName.isRoot) {
                continue
            }
            val classId = ClassId(currentPackageFqName.parent(), currentPackageFqName.shortName())
            val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: continue
            if (symbol.origin !is FirDeclarationOrigin.Source) {
                continue
            }
            // Should not be reported on regular dependent and friend dependent modules
            if (symbol.moduleData != declaration.moduleData &&
                !areDependentMultiplatformModules(symbol.moduleData, declaration.moduleData)
            ) {
                continue
            }
            reporter.reportOn(packageDirective.source, FirErrors.PACKAGE_CONFLICTS_WITH_CLASSIFIER, classId, context)
        }
    }

    private fun areDependentMultiplatformModules(module1: FirModuleData, module2: FirModuleData): Boolean {
        return module1 in module2.allDependsOnDependencies || module2 in module1.allDependsOnDependencies
    }

    private fun FqName.parentsIncludingSelf(): Sequence<FqName> {
        return generateSequence(this) { it.parentOrNull() }
    }
}