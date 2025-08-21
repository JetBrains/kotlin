/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.findNonInterfaceSupertype
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.StandardClassIds

object FirEnumClassSimpleChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (!declaration.isEnumClass) {
            return
        }

        declaration.findNonInterfaceSupertype()
            // Ignore Enum itself
            // If it's explicit, CLASS_CANNOT_BE_EXTENDED_DIRECTLY will be reported instead.
            // If it's implicit, it's fine.
            ?.takeUnless { it.coneType.fullyExpandedType().classId == StandardClassIds.Enum }
            ?.let { reporter.reportOn(it.source, FirErrors.CLASS_IN_SUPERTYPE_FOR_ENUM) }

        if (declaration.typeParameters.isNotEmpty()) {
            reporter.reportOn(declaration.typeParameters.firstOrNull()?.source ?: declaration.source, FirErrors.TYPE_PARAMETERS_IN_ENUM)
        }
    }
}
