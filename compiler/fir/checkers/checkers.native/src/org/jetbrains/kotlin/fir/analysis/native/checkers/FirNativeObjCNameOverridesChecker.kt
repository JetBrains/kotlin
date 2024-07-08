/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeObjCNameUtilities.checkCallableMember
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.isIntersectionOverride
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties

object FirNativeObjCNameOverridesChecker : FirClassChecker(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        // We just need to check intersection overrides, all other declarations are checked by FirNativeObjCNameChecker
        val firTypeScope = declaration.unsubstitutedScope(context)
        firTypeScope.processAllFunctions { symbol ->
            if (!symbol.isIntersectionOverride) return@processAllFunctions
            checkCallableMember(firTypeScope, symbol, declaration, context, reporter)
        }
        firTypeScope.processAllProperties { symbol ->
            if (!symbol.isIntersectionOverride) return@processAllProperties
            checkCallableMember(firTypeScope, symbol, declaration, context, reporter)
        }
    }
}
