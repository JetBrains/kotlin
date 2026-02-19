/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration


import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.WebCommonStandardClassIds
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.declarations.utils.isNativeObject
import org.jetbrains.kotlin.fir.types.ConeKotlinType

abstract class FirWebCommonAbstractNativeAnnotationChecker(
    private val requiredAnnotation: ClassId,
    private val error: KtDiagnosticFactory1<ConeKotlinType>,
) : FirSimpleFunctionChecker(MppCheckerKind.Platform) {

    context(context: CheckerContext)
    protected fun hasRequiredAnnotation(declaration: FirFunction): Boolean =
        declaration.hasAnnotation(requiredAnnotation, context.session)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        val annotation = declaration.getAnnotationByClassId(requiredAnnotation, context.session) ?: return

        val isMember = !context.isTopLevel && declaration.visibility != Visibilities.Local
        val isExtension = declaration.isExtension

        if (isMember && (isExtension || !declaration.symbol.isNativeObject(context.session)) || !isMember && !isExtension) {
            reporter.reportOn(
                declaration.source,
                error,
                annotation.resolvedType
            )
        }
    }
}
