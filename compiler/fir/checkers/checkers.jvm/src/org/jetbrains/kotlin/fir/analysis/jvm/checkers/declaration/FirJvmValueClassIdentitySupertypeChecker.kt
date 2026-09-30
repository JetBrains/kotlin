/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.isValhallaSupportEnabled
import org.jetbrains.kotlin.config.isValhallaValueClassFile
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.isFullValueClass
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement

// A Kotlin value class compiled without Valhalla value classes is an identity class, so a Valhalla value class can't extend it.
object FirJvmValueClassIdentitySupertypeChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (!declaration.symbol.isFullValueClass || !context.languageVersionSettings.isValhallaSupportEnabled()) return
        for (supertypeRef in declaration.superTypeRefs) {
            val supertypeSymbol = supertypeRef.toRegularClassSymbol(context.session) ?: continue
            if (!supertypeSymbol.isFullValueClass) continue
            val isValueClass = supertypeSymbol.declaresValueClassInClassFile()
                ?: supertypeSymbol.moduleData.session.languageVersionSettings.isValhallaSupportEnabled()
            if (!isValueClass) {
                reporter.reportOn(
                    supertypeRef.source, FirJvmErrors.VALUE_CLASS_EXTENDS_VALUE_CLASS_COMPILED_AS_IDENTITY_CLASS, supertypeRef.coneType
                )
            }
        }
    }
}

// Whether the class file of this class declares a value class, or null if the class isn't read from a class file.
internal fun FirRegularClassSymbol.declaresValueClassInClassFile(): Boolean? {
    @OptIn(SymbolInternals::class)
    val binaryClass = (fir.sourceElement as? KotlinJvmBinarySourceElement)?.binaryClass as? FileBasedKotlinClass ?: return null
    return isValhallaValueClassFile(binaryClass.majorVersion, binaryClass.usesPreviewFeatures(), binaryClass.hasIdentityFlag())
}
