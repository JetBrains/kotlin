/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.ACCIDENTAL_OVERLOAD_CLASH_BY_JVM_ERASURE
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ScopeFunctionRequiresPrewarm
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptorRepresentation
import org.jetbrains.kotlin.fir.scopes.processDirectlyOverriddenFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides

object FirOverrideSignatureClashChecker : FirSimpleFunctionChecker(MppCheckerKind.Platform) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirSimpleFunction) {
        if (!declaration.isOverride) return
        val name = declaration.name
        val containingClass = declaration.getContainingClass() ?: return

        val scope = containingClass.unsubstitutedScope(context)
        val overloads = mutableListOf<FirNamedFunctionSymbol>()
        scope.processFunctionsByName(name) {
            if (it !== declaration.symbol && it.valueParameterSymbols.size == declaration.valueParameters.size &&
                it.receiverParameterSymbol == null && it.contextParameterSymbols.isEmpty()
            ) {
                overloads += it
            }
        }
        if (overloads.isEmpty()) return

        @OptIn(ScopeFunctionRequiresPrewarm::class)
        scope.processDirectlyOverriddenFunctions(declaration.symbol) { overriddenSymbol ->
            for (overloadSymbol in overloads) {
                val unwrappedOverriddenSymbol = overriddenSymbol.unwrapSubstitutionOverrides()
                if (unwrappedOverriddenSymbol.receiverParameterSymbol != null ||
                    unwrappedOverriddenSymbol.contextParameterSymbols.isNotEmpty()
                ) {
                    continue
                }
                val indexedParameterTypes = unwrappedOverriddenSymbol.valueParameterSymbols.map { it.resolvedReturnType }.withIndex()
                if (indexedParameterTypes.none { it.value is ConeTypeParameterType }) continue
                if (indexedParameterTypes.all { (index, parameterType) ->
                        parameterType.computeJvmDescriptorRepresentation() ==
                                overloadSymbol.valueParameterSymbols[index].resolvedReturnType.computeJvmDescriptorRepresentation()
                    }
                ) {
                    reporter.reportOn(
                        declaration.source, ACCIDENTAL_OVERLOAD_CLASH_BY_JVM_ERASURE, unwrappedOverriddenSymbol, overloadSymbol
                    )
                    return@processDirectlyOverriddenFunctions ProcessorAction.STOP
                }
            }
            ProcessorAction.NEXT
        }
    }
}
