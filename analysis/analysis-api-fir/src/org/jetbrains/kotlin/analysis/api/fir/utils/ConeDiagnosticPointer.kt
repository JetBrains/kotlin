/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirTypeParameterSymbolBase
import org.jetbrains.kotlin.analysis.api.symbols.pointers.restoreSymbol
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeTypeVariableTypeIsNotInferred
import org.jetbrains.kotlin.fir.resolve.diagnostics.*

internal interface ConeDiagnosticPointer {
    companion object {
        context(analysisSession: KaFirSession)
        fun create(coneDiagnostic: ConeDiagnostic, guard: ConeTypeRecursionGuard): ConeDiagnosticPointer {
            return when (coneDiagnostic) {
                is ConeTypeVariableTypeIsNotInferred ->
                    ConeTypeVariableTypeIsNotInferredDiagnosticPointer(coneDiagnostic, analysisSession, guard)
                is ConeCannotInferTypeParameterType -> ConeCannotInferTypeParameterTypeDiagnosticPointer(coneDiagnostic, analysisSession)
                is ConeUnresolvedReferenceError -> ConeUnresolvedReferenceErrorDiagnosticPointer(coneDiagnostic)
                is ConeUnresolvedSymbolError -> ConeUnresolvedSymbolErrorDiagnosticPointer(coneDiagnostic)
                is ConeUnresolvedNameError -> ConeUnresolvedNameErrorDiagnosticPointer(coneDiagnostic)
                is ConeUnresolvedTypeQualifierError ->
                    ConeUnresolvedTypeQualifierErrorDiagnosticPointer(coneDiagnostic, analysisSession, guard)
                is ConeNoTypeArgumentsOnRhsError -> ConeNoTypeArgumentsOnRhsErrorDiagnosticPointer(coneDiagnostic, analysisSession)
                is ConeWrongNumberOfTypeArgumentsError ->
                    ConeWrongNumberOfTypeArgumentsErrorDiagnosticPointer(coneDiagnostic, analysisSession)
                else -> ConeGenericDiagnosticPointer(coneDiagnostic)
            }
        }
    }

    context(analysisSession: KaFirSession)
    fun restore(guard: ConeTypeRecursionGuard): ConeDiagnostic?
}

private class ConeCannotInferTypeParameterTypeDiagnosticPointer(
    coneDiagnostic: ConeCannotInferTypeParameterType,
    analysisSession: KaFirSession
) : ConeDiagnosticPointer {
    private val typeParameterPointer = analysisSession.firSymbolBuilder.classifierBuilder
        .buildTypeParameterSymbol(coneDiagnostic.typeParameter)
        .createPointer()
    private val reason = coneDiagnostic.reason

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeDiagnostic? {
        val typeParameterSymbol = typeParameterPointer.restoreSymbol() as? KaFirTypeParameterSymbolBase<*> ?: return null
        return ConeCannotInferTypeParameterType(typeParameterSymbol.firSymbol, reason)
    }
}

private class ConeTypeVariableTypeIsNotInferredDiagnosticPointer(
    coneDiagnostic: ConeTypeVariableTypeIsNotInferred,
    analysisSession: KaFirSession,
    guard: ConeTypeRecursionGuard,
) : ConeDiagnosticPointer {
    private val typePointer = coneDiagnostic.typeVariableType.createPointer(guard, analysisSession = analysisSession)
    private val reason = coneDiagnostic.reason

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeDiagnostic? {
        val typeVariableType = guard.restorePointer(typePointer) ?: return null
        return ConeTypeVariableTypeIsNotInferred(typeVariableType, reason)
    }
}

private class ConeUnresolvedReferenceErrorDiagnosticPointer(coneDiagnostic: ConeUnresolvedReferenceError) : ConeDiagnosticPointer {
    private val name = coneDiagnostic.name

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeDiagnostic? {
        return ConeUnresolvedReferenceError(name)
    }
}

private class ConeUnresolvedSymbolErrorDiagnosticPointer(coneDiagnostic: ConeUnresolvedSymbolError) : ConeDiagnosticPointer {
    private val classId = coneDiagnostic.classId

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeDiagnostic? {
        return ConeUnresolvedSymbolError(classId)
    }
}

private class ConeUnresolvedTypeQualifierErrorDiagnosticPointer(
    coneDiagnostic: ConeUnresolvedTypeQualifierError,
    analysisSession: KaFirSession,
    guard: ConeTypeRecursionGuard,
) : ConeDiagnosticPointer {
    private val qualifierPointers = coneDiagnostic.qualifiers.map { FirQualifierPartPointer(it, analysisSession, guard) }

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeDiagnostic? {
        val qualifiers = buildList {
            for (qualifierPointer in qualifierPointers) {
                val qualifier = qualifierPointer.restore(guard) ?: return null
                add(qualifier)
            }
        }

        return ConeUnresolvedTypeQualifierError(qualifiers)
    }
}

private class ConeUnresolvedNameErrorDiagnosticPointer(coneDiagnostic: ConeUnresolvedNameError) : ConeDiagnosticPointer {
    private val name = coneDiagnostic.name
    private val operatorToken = coneDiagnostic.operatorToken

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeDiagnostic? {
        return ConeUnresolvedNameError(name, operatorToken)
    }
}

private class ConeNoTypeArgumentsOnRhsErrorDiagnosticPointer(
    coneDiagnostic: ConeNoTypeArgumentsOnRhsError,
    analysisSession: KaFirSession
) : ConeDiagnosticPointer {
    private val desiredCount = coneDiagnostic.desiredCount
    private val symbolPointer = analysisSession.firSymbolBuilder.classifierBuilder
        .buildClassLikeSymbol(coneDiagnostic.symbol)
        .createPointer()

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeDiagnostic? {
        val symbol = symbolPointer.restoreSymbol() ?: return null
        return ConeNoTypeArgumentsOnRhsError(desiredCount, symbol.firSymbol)
    }
}

private class ConeWrongNumberOfTypeArgumentsErrorDiagnosticPointer(
    coneDiagnostic: ConeWrongNumberOfTypeArgumentsError,
    analysisSession: KaFirSession
) : ConeDiagnosticPointer {
    private val desiredCount = coneDiagnostic.desiredCount
    private val symbolPointer = analysisSession.firSymbolBuilder.classifierBuilder
        .buildClassLikeSymbol(coneDiagnostic.symbol)
        .createPointer()
    private val sourcePointer = coneDiagnostic.source.createPointer()
    private val forDeprecation = coneDiagnostic.isDeprecationErrorForCallableReferenceLhs

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeDiagnostic? {
        val symbol = symbolPointer.restoreSymbol() ?: return null
        val source = sourcePointer.restore() ?: return null
        return ConeWrongNumberOfTypeArgumentsError(desiredCount, symbol.firSymbol, source, forDeprecation)
    }
}

private class ConeGenericDiagnosticPointer(coneDiagnostic: ConeDiagnostic) : ConeDiagnosticPointer {
    private val reason = coneDiagnostic.reason

    context(analysisSession: KaFirSession)
    override fun restore(guard: ConeTypeRecursionGuard): ConeDiagnostic? {
        return ConeSimpleDiagnostic(reason)
    }
}
