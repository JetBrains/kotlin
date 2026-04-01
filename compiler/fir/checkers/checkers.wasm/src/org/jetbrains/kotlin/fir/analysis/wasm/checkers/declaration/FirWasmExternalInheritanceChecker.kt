/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.StandardClassIds

sealed class FirWasmExternalInheritanceChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
    object Regular : FirWasmExternalInheritanceChecker(MppCheckerKind.Platform) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (declaration.isExpect) return
            super.check(declaration)
        }
    }

    object ForExpectClass : FirWasmExternalInheritanceChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (!declaration.isExpect) return
            super.check(declaration)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val session = context.session
        val isCurrentClassExternal = declaration.symbol.isEffectivelyExternal(session)
        for (superTypeRef in declaration.superTypeRefs) {
            val superClass = (superTypeRef.toClassLikeSymbol(session)?.fullyExpandedClass() ?: continue)
            if (superClass.classId == StandardClassIds.Any) continue  // External classes can extend Any

            val isSuperClassExternal = superClass.isEffectivelyExternal(session)
            if (!isCurrentClassExternal && isSuperClassExternal) {
                reporter.reportOn(
                    declaration.source,
                    FirWasmErrors.NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE,
                    superTypeRef.coneType
                )
            } else if (isCurrentClassExternal && !isSuperClassExternal) {
                // External enum and annotation classes are prohibited, but they add implicit non-external super types. Skip reporting errors for them.
                if (declaration.classKind == ClassKind.ANNOTATION_CLASS && superClass.classId == StandardClassIds.Annotation) continue
                if (declaration.classKind == ClassKind.ENUM_CLASS && superClass.classId == StandardClassIds.Enum) continue
                if (superClass.isExpect) continue

                reporter.reportOn(
                    declaration.source,
                    FirWebCommonErrors.EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE,
                    superTypeRef.coneType
                )
            }
        }
    }
}
