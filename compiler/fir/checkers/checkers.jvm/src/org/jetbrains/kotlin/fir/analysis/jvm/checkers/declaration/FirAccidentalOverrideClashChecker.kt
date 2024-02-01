/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.isHiddenToOvercomeSignatureClash
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.initialSignatureAttr
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.JVM_SHORT_NAME_TO_BUILTIN_SHORT_NAMES_MAP
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.sameAsBuiltinMethodWithErasedValueParameters

object FirAccidentalOverrideClashChecker : FirSimpleFunctionChecker(MppCheckerKind.Platform) {
    override fun check(
        declaration: FirSimpleFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (!declaration.isOverride) return
        val name = declaration.name
        val mayBeRenamedBuiltIn = name in namesPossibleForRenamedBuiltin
        val mayBeSameAsBuiltInWithErasedParameters = name.sameAsBuiltinMethodWithErasedValueParameters
        if (!mayBeRenamedBuiltIn && !mayBeSameAsBuiltInWithErasedParameters) return
        val containingClass = declaration.getContainingClass(context.session) ?: return

        var reported = false
        containingClass.unsubstitutedScope(context).processFunctionsByName(name) {
            @OptIn(SymbolInternals::class)
            val hiddenFir = it.fir
            if (!reported && hiddenFir.isHiddenToOvercomeSignatureClash == true) {
                if (declaration.computeJvmDescriptor() == hiddenFir.computeJvmDescriptor()) {
                    val regularBase = hiddenFir.initialSignatureAttr as? FirSimpleFunction ?: return@processFunctionsByName
                    val description = when {
                        mayBeRenamedBuiltIn -> "a renamed function"
                        else -> "a function with erased parameters"
                    }
                    reporter.reportOn(
                        declaration.source, ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE, it, description, regularBase.symbol, context
                    )
                    reported = true
                }
            }
        }
    }

    private val namesPossibleForRenamedBuiltin = JVM_SHORT_NAME_TO_BUILTIN_SHORT_NAMES_MAP.values.toSet()
}
