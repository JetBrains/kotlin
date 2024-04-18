/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer.checker

import org.jetbrains.kotlin.backend.common.actualizer.reportActualAnnotationsNotMatchExpect
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualAnnotationMatchChecker

internal object IrExpectActualAnnotationMatchingChecker : IrExpectActualChecker {

    override fun check(context: IrExpectActualChecker.Context) = with(context) {
        val languageVersionSettings = diagnosticsReporter.languageVersionSettings
        if (!languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) {
            return
        }

        for ((expectSymbol, actualSymbol) in matchedExpectToActual.entries) {
            if (expectSymbol is IrTypeParameterSymbol) {
                continue
            }
            val incompatibility = AbstractExpectActualAnnotationMatchChecker
                .areAnnotationsCompatible(expectSymbol, actualSymbol, containingExpectClass = null, matchingContext) ?: continue

            // If `actualSymbol` is obtained from a builtins provider, its file is null
            // In this case; the `expectSymbol` is used to prevent from crashing
            // Currently it's relevant only while compiling stdlib
            val reportOn = (getTypealiasSymbolIfActualizedViaTypealias(expectSymbol.owner as IrDeclaration, classActualizationInfo)
                ?: getContainingActualClassIfFakeOverride(actualSymbol)
                ?: actualSymbol)
                .takeIf {
                    !languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) || (it.owner as IrDeclaration).fileOrNull != null
                }
                ?: expectSymbol
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

    private fun getContainingActualClassIfFakeOverride(actualSymbol: IrSymbol): IrSymbol? {
        if (!actualSymbol.isFakeOverride) {
            return null
        }
        return getContainingTopLevelClass(actualSymbol.owner as IrDeclaration)?.symbol
    }
}