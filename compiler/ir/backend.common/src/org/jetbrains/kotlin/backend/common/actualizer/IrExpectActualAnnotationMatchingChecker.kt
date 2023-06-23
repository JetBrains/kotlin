/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.lower.parentsWithSelf
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualAnnotationMatchChecker

internal class IrExpectActualAnnotationMatchingChecker(
    private val matchedExpectToActual: Map<IrSymbol, IrSymbol>,
    private val classActualizationInfo: ClassActualizationInfo,
    typeSystemContext: IrTypeSystemContext,
    private val diagnosticsReporter: KtDiagnosticReporterWithImplicitIrBasedContext,
) {
    private val context = object : IrExpectActualMatchingContext(typeSystemContext, classActualizationInfo.actualClasses) {
        override fun onMatchedClasses(expectClassSymbol: IrClassSymbol, actualClassSymbol: IrClassSymbol) {
            error("Must not be called")
        }

        override fun onMatchedCallables(expectSymbol: IrSymbol, actualSymbol: IrSymbol) {
            error("Must not be called")
        }
    }

    fun check() {
        for ((expectSymbol, actualSymbol) in matchedExpectToActual.entries) {
            if (expectSymbol.isFakeOverride || actualSymbol.isFakeOverride) {
                continue
            }
            val incompatibility =
                AbstractExpectActualAnnotationMatchChecker.areAnnotationsCompatible(expectSymbol, actualSymbol, context) ?: continue

            val reportOn = getTypealiasSymbolIfActualizedViaTypealias(expectSymbol) ?: actualSymbol
            diagnosticsReporter.reportActualAnnotationsNotMatchExpect(
                incompatibility.expectSymbol as IrSymbol,
                incompatibility.actualSymbol as IrSymbol,
                reportOn,
            )
        }
    }

    private val IrSymbol.isFakeOverride: Boolean
        get() = (owner as IrDeclaration).isFakeOverride

    private fun getTypealiasSymbolIfActualizedViaTypealias(expectSymbol: IrSymbol): IrTypeAliasSymbol? {
        val expectDeclaration = expectSymbol.owner as IrDeclaration
        val topLevelExpectClass = expectDeclaration.parentsWithSelf.filterIsInstance<IrClass>().lastOrNull() ?: return null
        val classId = topLevelExpectClass.classIdOrFail
        return classActualizationInfo.actualTypeAliases[classId]
    }
}