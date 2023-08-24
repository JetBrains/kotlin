/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
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
            if (expectSymbol is IrTypeParameterSymbol) {
                continue
            }
            if (expectSymbol.isFakeOverride) {
                continue
            }
            val incompatibility =
                AbstractExpectActualAnnotationMatchChecker.areAnnotationsCompatible(expectSymbol, actualSymbol, context) ?: continue

            val reportOn = getTypealiasSymbolIfActualizedViaTypealias(expectSymbol)
                ?: getContainingActualClassIfFakeOverride(actualSymbol)
                ?: actualSymbol
            diagnosticsReporter.reportActualAnnotationsNotMatchExpect(
                incompatibility.expectSymbol as IrSymbol,
                incompatibility.actualSymbol as IrSymbol,
                incompatibility.type.mapAnnotationType { it.annotationSymbol as IrConstructorCall },
                reportOn,
            )
        }
    }

    private val IrSymbol.isFakeOverride: Boolean
        get() = (owner as IrDeclaration).isFakeOverride

    private fun getTypealiasSymbolIfActualizedViaTypealias(expectSymbol: IrSymbol): IrTypeAliasSymbol? {
        val topLevelExpectClass = getContainingTopLevelClass(expectSymbol) ?: return null
        val classId = topLevelExpectClass.classIdOrFail
        return classActualizationInfo.actualTypeAliases[classId]
    }

    private fun getContainingActualClassIfFakeOverride(actualSymbol: IrSymbol): IrSymbol? {
        if (!actualSymbol.isFakeOverride) {
            return null
        }
        return getContainingTopLevelClass(actualSymbol)?.symbol
    }

    private fun getContainingTopLevelClass(symbol: IrSymbol): IrClass? {
        val declaration = symbol.owner as IrDeclaration
        val parentsWithSelf = sequenceOf(declaration) + declaration.parents
        return parentsWithSelf.filterIsInstance<IrClass>().lastOrNull()
    }
}