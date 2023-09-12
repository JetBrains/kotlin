/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.renderer.ConeAttributeRenderer
import org.jetbrains.kotlin.fir.renderer.ConeIdFullRenderer
import org.jetbrains.kotlin.fir.renderer.ConeTypeRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.FirTypeRef

/**
 * **Note**: the signature doesn't contain a name. This check should be done externally.
 */
internal class FirCallableSignature private constructor(
    private val receiverType: String?,
    private val contextReceiverTypes: List<String>,
    private val parameters: List<String>?,
    private val typeParametersCount: Int,
    private val returnType: String,
) {
    fun hasTheSameSignature(declaration: FirCallableSymbol<*>): Boolean = hasTheSameSignature(declaration.fir)

    fun hasTheSameSignature(declaration: FirCallableDeclaration): Boolean {
        if ((receiverType == null) != (declaration.receiverParameter == null)) return false
        if (contextReceiverTypes.size != declaration.contextReceivers.size) return false
        if (typeParametersCount != declaration.typeParameters.size) return false
        if (parameters?.size != (declaration as? FirFunction)?.valueParameters?.size) return false

        declaration.lazyResolveToPhase(FirResolvePhase.TYPES)
        if (receiverType != declaration.receiverParameter?.typeRef?.renderType()) return false

        val receivers = declaration.contextReceivers
        for ((index, parameter) in contextReceiverTypes.withIndex()) {
            if (receivers[index].typeRef.renderType() != parameter) return false
        }

        if (declaration is FirFunction) {
            requireNotNull(parameters)
            for ((index, parameter) in declaration.valueParameters.withIndex()) {
                if (parameters[index] != parameter.returnTypeRef.renderType()) return false
            }
        }

        return returnType == declaration.symbol.resolvedReturnTypeRef.renderType()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FirCallableSignature) return false

        if (receiverType != other.receiverType) return false
        if (contextReceiverTypes != other.contextReceiverTypes) return false
        if (parameters != other.parameters) return false
        if (typeParametersCount != other.typeParametersCount) return false
        return returnType == other.returnType

    }

    override fun hashCode(): Int {
        var result = receiverType?.hashCode() ?: 0
        result = 31 * result + contextReceiverTypes.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + typeParametersCount.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }

    companion object {
        fun createSignature(callableSymbol: FirCallableSymbol<*>): FirCallableSignature = createSignature(callableSymbol.fir)

        fun createSignature(callableDeclaration: FirCallableDeclaration): FirCallableSignature {
            callableDeclaration.lazyResolveToPhase(FirResolvePhase.TYPES)

            return FirCallableSignature(
                receiverType = callableDeclaration.receiverParameter?.typeRef?.renderType(),
                contextReceiverTypes = callableDeclaration.contextReceivers.map { it.typeRef.renderType() },
                parameters = if (callableDeclaration is FirFunction) {
                    callableDeclaration.valueParameters.map { it.returnTypeRef.renderType() }
                } else {
                    null
                },
                typeParametersCount = callableDeclaration.typeParameters.size,
                returnType = callableDeclaration.symbol.resolvedReturnTypeRef.renderType(),
            )
        }
    }
}

private fun FirTypeRef.renderType(builder: StringBuilder = StringBuilder()): String = FirRenderer(
    builder = builder,
    annotationRenderer = null,
    bodyRenderer = null,
    callArgumentsRenderer = null,
    classMemberRenderer = null,
    contractRenderer = null,
    declarationRenderer = null,
    idRenderer = ConeIdFullRenderer(),
    modifierRenderer = null,
    packageDirectiveRenderer = null,
    propertyAccessorRenderer = null,
    resolvePhaseRenderer = null,
    typeRenderer = ConeTypeRenderer(attributeRenderer = EmptyConeTypeAttributeRenderer),
    valueParameterRenderer = null,
    errorExpressionRenderer = null,
).renderElementAsString(this)

private object EmptyConeTypeAttributeRenderer : ConeAttributeRenderer() {
    override fun render(attributes: Iterable<ConeAttribute<*>>): String = ""
}
