/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirTypeParameterSymbolBase
import org.jetbrains.kotlin.analysis.api.symbols.pointers.restoreSymbol
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeTypeVariableTypeIsNotInferred
import org.jetbrains.kotlin.fir.resolve.diagnostics.*

internal interface ConeDiagnosticPointer {
    companion object {
        fun create(coneDiagnostic: ConeDiagnostic, builder: KaSymbolByFirBuilder): ConeDiagnosticPointer {
            return when (coneDiagnostic) {
                is ConeTypeVariableTypeIsNotInferred -> ConeTypeVariableTypeIsNotInferredDiagnosticPointer(coneDiagnostic, builder)
                is ConeCannotInferTypeParameterType -> ConeCannotInferTypeParameterTypeDiagnosticPointer(coneDiagnostic, builder)
                is ConeUnresolvedReferenceError -> ConeUnresolvedReferenceErrorDiagnosticPointer(coneDiagnostic)
                is ConeUnresolvedSymbolError -> ConeUnresolvedSymbolErrorDiagnosticPointer(coneDiagnostic)
                is ConeUnresolvedNameError -> ConeUnresolvedNameErrorDiagnosticPointer(coneDiagnostic)
                is ConeUnresolvedTypeQualifierError -> ConeUnresolvedTypeQualifierErrorDiagnosticPointer(coneDiagnostic, builder)
                is ConeNoTypeArgumentsOnRhsError -> ConeNoTypeArgumentsOnRhsErrorDiagnosticPointer(coneDiagnostic, builder)
                is ConeWrongNumberOfTypeArgumentsError -> ConeWrongNumberOfTypeArgumentsErrorDiagnosticPointer(coneDiagnostic, builder)
                else -> ConeGenericDiagnosticPointer(coneDiagnostic)
            }
        }
    }

    context(analysisSession: KaFirSession)
    fun restore(): ConeDiagnostic?
}

private class ConeCannotInferTypeParameterTypeDiagnosticPointer(
    coneDiagnostic: ConeCannotInferTypeParameterType,
    builder: KaSymbolByFirBuilder
) : ConeDiagnosticPointer {
    private val typeParameterPointer = builder.classifierBuilder.buildTypeParameterSymbol(coneDiagnostic.typeParameter).createPointer()
    private val reason = coneDiagnostic.reason

    context(analysisSession: KaFirSession)
    override fun restore(): ConeDiagnostic? {
        val typeParameterSymbol = typeParameterPointer.restoreSymbol() as? KaFirTypeParameterSymbolBase<*> ?: return null
        return ConeCannotInferTypeParameterType(typeParameterSymbol.firSymbol, reason)
    }
}

private class ConeTypeVariableTypeIsNotInferredDiagnosticPointer(
    coneDiagnostic: ConeTypeVariableTypeIsNotInferred,
    builder: KaSymbolByFirBuilder
) : ConeDiagnosticPointer {
    private val typePointer = coneDiagnostic.typeVariableType.createPointer(builder)
    private val reason = coneDiagnostic.reason

    context(analysisSession: KaFirSession)
    override fun restore(): ConeDiagnostic? {
        val typeVariableType = typePointer.restore() ?: return null
        return ConeTypeVariableTypeIsNotInferred(typeVariableType, reason)
    }
}

private class ConeUnresolvedReferenceErrorDiagnosticPointer(coneDiagnostic: ConeUnresolvedReferenceError) : ConeDiagnosticPointer {
    private val name = coneDiagnostic.name

    context(analysisSession: KaFirSession)
    override fun restore(): ConeDiagnostic? {
        return ConeUnresolvedReferenceError(name)
    }
}

private class ConeUnresolvedSymbolErrorDiagnosticPointer(coneDiagnostic: ConeUnresolvedSymbolError) : ConeDiagnosticPointer {
    private val classId = coneDiagnostic.classId

    context(analysisSession: KaFirSession)
    override fun restore(): ConeDiagnostic? {
        return ConeUnresolvedSymbolError(classId)
    }
}

private class ConeUnresolvedTypeQualifierErrorDiagnosticPointer(
    coneDiagnostic: ConeUnresolvedTypeQualifierError,
    builder: KaSymbolByFirBuilder
) : ConeDiagnosticPointer {
    private val qualifierPointers = coneDiagnostic.qualifiers.map { FirQualifierPartPointer(it, builder) }

    context(analysisSession: KaFirSession)
    override fun restore(): ConeDiagnostic? {
        val qualifiers = buildList {
            for (qualifierPointer in qualifierPointers) {
                val qualifier = qualifierPointer.restore() ?: return null
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
    override fun restore(): ConeDiagnostic? {
        return ConeUnresolvedNameError(name, operatorToken)
    }
}

private class ConeNoTypeArgumentsOnRhsErrorDiagnosticPointer(
    coneDiagnostic: ConeNoTypeArgumentsOnRhsError,
    builder: KaSymbolByFirBuilder
) : ConeDiagnosticPointer {
    private val desiredCount = coneDiagnostic.desiredCount
    private val symbolPointer = builder.classifierBuilder.buildClassLikeSymbol(coneDiagnostic.symbol).createPointer()

    context(analysisSession: KaFirSession)
    override fun restore(): ConeDiagnostic? {
        val symbol = symbolPointer.restoreSymbol() ?: return null
        return ConeNoTypeArgumentsOnRhsError(desiredCount, symbol.firSymbol)
    }
}

private class ConeWrongNumberOfTypeArgumentsErrorDiagnosticPointer(
    coneDiagnostic: ConeWrongNumberOfTypeArgumentsError,
    builder: KaSymbolByFirBuilder
) : ConeDiagnosticPointer {
    private val desiredCount = coneDiagnostic.desiredCount
    private val symbolPointer = builder.classifierBuilder.buildClassLikeSymbol(coneDiagnostic.symbol).createPointer()
    private val sourcePointer = coneDiagnostic.source.createPointer()
    private val forDeprecation = coneDiagnostic.isDeprecationErrorForCallableReferenceLhs

    context(analysisSession: KaFirSession)
    override fun restore(): ConeDiagnostic? {
        val symbol = symbolPointer.restoreSymbol() ?: return null
        val source = sourcePointer.restore() ?: return null
        return ConeWrongNumberOfTypeArgumentsError(desiredCount, symbol.firSymbol, source, forDeprecation)
    }
}

private class ConeGenericDiagnosticPointer(coneDiagnostic: ConeDiagnostic) : ConeDiagnosticPointer {
    private val reason = coneDiagnostic.reason

    context(analysisSession: KaFirSession)
    override fun restore(): ConeDiagnostic? {
        return ConeSimpleDiagnostic(reason)
    }
}
