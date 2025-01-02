/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.isNonReifiedTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.overriddenFunctions
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByTypeParameterErasure
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker

private data class MethodSignature(
    val originalFunction: FirNamedFunctionSymbol,
    val returnType: ConeKotlinType,
    val parameters: List<ConeKotlinType>
) {
    val name: Name get() = originalFunction.name
}

object BridgeMethodChecker : FirSimpleFunctionChecker(MppCheckerKind.Platform) {
    private fun convertToBridgeMethodSignatureIfNecessary(originalFunction: FirNamedFunctionSymbol, context: CheckerContext): MethodSignature? {
        val containingClass = originalFunction.getContainingClassSymbol() as? FirClassSymbol<*> ?: return null
        //TODO: filter only (1) directly implemented interfaces or (2) interfaces extended by (1) or (2) and check that it belongs to them
        val lastOverride = originalFunction.overriddenFunctions(containingClass, context).lastOrNull() ?: return null
        if (!lastOverride.valueParameterSymbols.any { it.resolvedReturnType.isNonReifiedTypeParameter() }) return null
        if (lastOverride.getContainingClassSymbol()!!.classKind != ClassKind.INTERFACE) return null
        val map = substitutorByTypeParameterErasure(context.session)
        val returnType = map.substituteOrSelf(lastOverride.resolvedReturnType)
        val parameters = lastOverride.valueParameterSymbols.map { map.substituteOrSelf(it.resolvedReturnType) }
        return MethodSignature(originalFunction, returnType, parameters)
    }

    @OptIn(SymbolInternals::class)
    private fun convertToTypeErasedMethodSignature(originalFunction: FirNamedFunctionSymbol, context: CheckerContext): MethodSignature {
        val map = substitutorByTypeParameterErasure(context.session)
        val returnType = map.substituteOrSelf(originalFunction.fir.returnTypeRef.resolveIfJavaType(context.session, JavaTypeParameterStack.EMPTY, source = null).coneType)
        val parameters = originalFunction.valueParameterSymbols.map { map.substituteOrSelf(it.fir.returnTypeRef.resolveIfJavaType(context.session, JavaTypeParameterStack.EMPTY, source = null).coneType) }
        return MethodSignature(originalFunction, returnType, parameters)
    }

    private fun checkOverride(base: MethodSignature, derived: MethodSignature, context: CheckerContext): Boolean {
        if (base.name != derived.name)
            return false
        if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, base.returnType, derived.returnType))
            return false
        if (base.parameters.count() != derived.parameters.count())
            return false
        if (derived.parameters.zip(base.parameters).all { (a, b) -> AbstractTypeChecker.equalTypes(context.session.typeContext, a, b) })
            return true
        return false
    }

    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val bridgeMethodSignature = convertToBridgeMethodSignatureIfNecessary(declaration.symbol, context) ?: return
        val superTypes = declaration.symbol.getContainingClassSymbol()!!.getSuperTypes(context.session, true, false, false)
        superTypes.forEach { type ->
            type.toRegularClassSymbol(context.session)!!.declarationSymbols.forEach { decl ->
                if (decl !is FirNamedFunctionSymbol) return@forEach
                val typeErasedMethodSignature = convertToTypeErasedMethodSignature(decl, context)
                if (checkOverride(bridgeMethodSignature, typeErasedMethodSignature, context)) {
                    reporter.reportOn(
                        declaration.source,
                        FirJvmErrors.INHERITED_FUNCTION_NAME_CLASH_WITH_BRIDGE_METHOD,
                        bridgeMethodSignature.originalFunction,
                        typeErasedMethodSignature.originalFunction,
                        context
                    )
                    return
                }
            }
        }
    }
}
