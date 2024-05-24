/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol

object FirPropertyInitializationAnalyzer : AbstractFirPropertyInitializationChecker(MppCheckerKind.Common) {
    override fun analyze(data: PropertyInitializationInfoData, reporter: DiagnosticReporter, context: CheckerContext) {
        PropertyInitializationCheckProcessor.check(data, isForInitialization = false, context, reporter)
    }
}

val FirDeclaration.evaluatedInPlace: Boolean
    get() = when (this) {
        is FirAnonymousFunction -> invocationKind.isInPlace
        is FirAnonymousObject -> classKind != ClassKind.ENUM_ENTRY
        is FirConstructor -> true // child of class initialization graph
        is FirFunction, is FirClass -> false
        else -> true // property initializer, etc.
    }

/**
 * [isForInitialization] means that caller is interested in member property in the scope
 * of file or class initialization section. In this case the fact that property has
 * initializer does not mean that it's safe to access this property in any place:
 *
 * ```
 * class A {
 *     val b = a // a is not initialized here
 *     val a = 10
 *     val c = a // but initialized here
 * }
 * ```
 */
@OptIn(SymbolInternals::class)
fun FirPropertySymbol.requiresInitialization(isForInitialization: Boolean): Boolean {
    val hasImplicitBackingField = !hasExplicitBackingField && hasBackingField
    return when {
        this is FirSyntheticPropertySymbol -> false
        isForInitialization -> hasDelegate || hasImplicitBackingField
        else -> !hasInitializer && hasImplicitBackingField && fir.isCatchParameter != true
    }
}


object PropertyInitializationCheckProcessor : VariableInitializationCheckProcessor()
