/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeTypeVariableTypeIsNotInferred
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeNoTypeArgumentsOnRhsError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedReferenceError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedTypeQualifierError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeWrongNumberOfTypeArgumentsError
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.fir.types.toConeTypeProjection

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

    fun restore(@Suppress("unused") session: KaFirSession): ConeDiagnostic?
}

private class ConeCannotInferTypeParameterTypeDiagnosticPointer(
    coneDiagnostic: ConeCannotInferTypeParameterType,
    builder: KaSymbolByFirBuilder
) : ConeDiagnosticPointer {
    private val typeParameterPointer = builder.classifierBuilder.buildTypeParameterSymbol(coneDiagnostic.typeParameter).createPointer()
    private val reason = coneDiagnostic.reason

    override fun restore(session: KaFirSession): ConeDiagnostic? {
        val typeParameterSymbol = with(session) { typeParameterPointer.restoreSymbol() } as? KaFirTypeParameterSymbol ?: return null
        return ConeCannotInferTypeParameterType(typeParameterSymbol.firSymbol, reason)
    }
}

@OptIn(KaAnalysisNonPublicApi::class)
private class ConeTypeVariableTypeIsNotInferredDiagnosticPointer(
    coneDiagnostic: ConeTypeVariableTypeIsNotInferred,
    builder: KaSymbolByFirBuilder
) : ConeDiagnosticPointer {
    private val typePointer = builder.typeBuilder.buildKtType(coneDiagnostic.typeVariableType).createPointer()
    private val reason = coneDiagnostic.reason

    override fun restore(session: KaFirSession): ConeDiagnostic? {
        val restoredType = typePointer.restore(session) as? KaFirType ?: return null
        val restoredConeType = restoredType.coneType as? ConeTypeVariableType ?: return null
        return ConeTypeVariableTypeIsNotInferred(restoredConeType, reason)
    }
}

private class ConeUnresolvedReferenceErrorDiagnosticPointer(coneDiagnostic: ConeUnresolvedReferenceError) : ConeDiagnosticPointer {
    private val name = coneDiagnostic.name

    override fun restore(session: KaFirSession): ConeDiagnostic? {
        return ConeUnresolvedReferenceError(name)
    }
}

private class ConeUnresolvedSymbolErrorDiagnosticPointer(coneDiagnostic: ConeUnresolvedSymbolError) : ConeDiagnosticPointer {
    private val classId = coneDiagnostic.classId

    override fun restore(session: KaFirSession): ConeDiagnostic? {
        return ConeUnresolvedSymbolError(classId)
    }
}

private class ConeUnresolvedTypeQualifierErrorDiagnosticPointer(
    coneDiagnostic: ConeUnresolvedTypeQualifierError,
    builder: KaSymbolByFirBuilder
) : ConeDiagnosticPointer {
    private val qualifierPointers = coneDiagnostic.qualifiers.map { FirQualifierPartPointer(it, builder) }
    private val isNullable = coneDiagnostic.isNullable

    override fun restore(session: KaFirSession): ConeDiagnostic? {
        val qualifiers = buildList {
            for (qualifierPointer in qualifierPointers) {
                val qualifier = qualifierPointer.restore(session) ?: return null
                add(qualifier)
            }
        }

        return ConeUnresolvedTypeQualifierError(qualifiers, isNullable)
    }
}

@OptIn(KaAnalysisNonPublicApi::class)
private class FirQualifierPartPointer(qualifierPart: FirQualifierPart, builder: KaSymbolByFirBuilder) {
    private val name = qualifierPart.name
    private val typeArgumentPointers = qualifierPart.typeArgumentList.typeArguments
        .map { ConeTypeProjectionPointer(it.toConeTypeProjection(), builder) }

    fun restore(session: KaFirSession): FirQualifierPart? {
        val firTypeArgumentList = FirTypeArgumentListImpl(source = null).apply {
            for (typeArgumentPointer in typeArgumentPointers) {
                val coneTypeArgument = typeArgumentPointer.restore(session) ?: return null

                val typeArgument = when (coneTypeArgument) {
                    ConeStarProjection -> buildStarProjection()
                    is ConeKotlinTypeProjection -> {
                        buildTypeProjectionWithVariance {
                            typeRef = buildResolvedTypeRef { this.type = coneTypeArgument.type }
                            variance = coneTypeArgument.kind.toVariance()
                        }
                    }
                    else -> error("Unexpected type argument kind: $coneTypeArgument")
                }

                typeArguments.add(typeArgument)
            }
        }

        return FirQualifierPartImpl(source = null, name, firTypeArgumentList)
    }
}

private class ConeUnresolvedNameErrorDiagnosticPointer(coneDiagnostic: ConeUnresolvedNameError) : ConeDiagnosticPointer {
    private val name = coneDiagnostic.name
    private val operatorToken = coneDiagnostic.operatorToken

    override fun restore(session: KaFirSession): ConeDiagnostic? {
        return ConeUnresolvedNameError(name, operatorToken)
    }
}

private class ConeNoTypeArgumentsOnRhsErrorDiagnosticPointer(
    coneDiagnostic: ConeNoTypeArgumentsOnRhsError,
    builder: KaSymbolByFirBuilder
) : ConeDiagnosticPointer {
    private val desiredCount = coneDiagnostic.desiredCount
    private val symbolPointer = builder.classifierBuilder.buildClassLikeSymbol(coneDiagnostic.symbol).createPointer()

    override fun restore(session: KaFirSession): ConeDiagnostic? {
        val symbol = with(session) { symbolPointer.restoreSymbol() } ?: return null
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

    override fun restore(session: KaFirSession): ConeDiagnostic? {
        val symbol = with(session) { symbolPointer.restoreSymbol() } ?: return null
        val source = sourcePointer.restore() ?: return null
        return ConeWrongNumberOfTypeArgumentsError(desiredCount, symbol.firSymbol, source)
    }
}

private class ConeGenericDiagnosticPointer(coneDiagnostic: ConeDiagnostic) : ConeDiagnosticPointer {
    private val reason = coneDiagnostic.reason

    override fun restore(session: KaFirSession): ConeDiagnostic? {
        return ConeSimpleDiagnostic(reason)
    }
}