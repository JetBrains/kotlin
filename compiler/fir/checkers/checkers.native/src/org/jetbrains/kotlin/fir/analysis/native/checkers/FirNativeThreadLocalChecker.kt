/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object FirNativeThreadLocalChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    private val threadLocalClassId = ClassId.topLevel(FqName("kotlin.native.concurrent.ThreadLocal"))

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val isObject = declaration is FirClass && declaration.classKind == ClassKind.OBJECT
        val isOk = declaration is FirVariable &&
                (declaration is FirProperty && declaration.hasBackingField || declaration.delegate != null) || isObject
        if (!isOk) {
            reporter.reportIfHasAnnotation(declaration, threadLocalClassId, FirNativeErrors.INAPPLICABLE_THREAD_LOCAL, context)
        }

        if (declaration.source?.kind is KtFakeSourceElementKind) return

        if (!context.isTopLevel && !isObject) {
            reporter.reportIfHasAnnotation(declaration, threadLocalClassId, FirNativeErrors.INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL, context)
        }
    }
}
