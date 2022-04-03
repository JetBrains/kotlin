/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSuspendFunctionType
import org.jetbrains.kotlin.name.JvmNames.SYNCHRONIZED_ANNOTATION_CLASS_ID

object FirSynchronizedAnnotationChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation = declaration.getAnnotationByClassId(SYNCHRONIZED_ANNOTATION_CLASS_ID) ?: return

        if (declaration.isInline) {
            reporter.reportOn(annotation.source, FirJvmErrors.SYNCHRONIZED_ON_INLINE, context)
            return
        }
        if (declaration.isSuspend ||
            (declaration as? FirAnonymousFunction)?.typeRef?.coneType?.isSuspendFunctionType(context.session) == true
        ) {
            reporter.reportOn(annotation.source, FirJvmErrors.SYNCHRONIZED_ON_SUSPEND, context)
            return
        }

        val containingClass = declaration.getContainingClassSymbol(context.session) ?: return
        if (containingClass.classKind == ClassKind.INTERFACE) {
            reporter.reportOn(annotation.source, FirJvmErrors.SYNCHRONIZED_IN_INTERFACE, context)
        } else if (declaration.isAbstract) {
            reporter.reportOn(annotation.source, FirJvmErrors.SYNCHRONIZED_ON_ABSTRACT, context)
        }
    }
}