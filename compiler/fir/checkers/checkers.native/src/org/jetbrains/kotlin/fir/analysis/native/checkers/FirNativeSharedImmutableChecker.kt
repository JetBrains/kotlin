/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object FirNativeSharedImmutableChecker : FirBasicDeclarationChecker() {
    private val sharedImmutableClassId = ClassId.topLevel(FqName("kotlin.native.concurrent.SharedImmutable"))

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirVariable) {
            if (declaration !is FirValueParameter || context.containingDeclarations.lastOrNull() !is FirPrimaryConstructor) {
                val hasBackingField = declaration is FirProperty && declaration.hasBackingField
                if ((declaration.isVar || !hasBackingField) && declaration.delegate == null) {
                    reporter.reportIfHasAnnotation(
                        declaration,
                        sharedImmutableClassId,
                        FirNativeErrors.INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY,
                        context
                    )
                }
            }
        } else {
            if (declaration.source?.kind is KtFakeSourceElementKind) return

            reporter.reportIfHasAnnotation(
                declaration,
                sharedImmutableClassId,
                FirNativeErrors.INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY,
                context
            )

            return
        }

        if (!context.isTopLevel) {
            reporter.reportIfHasAnnotation(
                declaration,
                sharedImmutableClassId,
                FirNativeErrors.INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL,
                context
            )
        }
    }
}
