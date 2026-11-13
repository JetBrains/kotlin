/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.renderer.ConeAttributeRenderer
import org.jetbrains.kotlin.fir.renderer.ConeIdFullRenderer
import org.jetbrains.kotlin.fir.renderer.ConeTypeRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.AbbreviatedTypeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.FirTypeRef

/**
 * The signature of a callable declaration, used to find the declaration among the callables with the same name, e.g., to restore a
 * symbol pointer. The name isn't a part of the signature, so it should be checked separately.
 */
@KaImplementationDetail
class FirCallableSignature private constructor(
    private val receiverType: String?,
    private val contextParameterTypes: List<String>,
    private val valueParameterTypes: List<String>?,
    /**
     * The names and bounds of the type parameters. Types render type parameters by name only, so the bounds tell apart overloads
     * such as:
     *
     * ```kotlin
     * fun <T : A> foo(x: T)
     * fun <T : B> foo(x: T)
     * ```
     *
     * The names tell apart overloads whose bounds are the same by position:
     *
     * ```kotlin
     * fun <T : A, U : B> foo(x: T, y: U)
     * fun <U : A, T : B> foo(x: T, y: U)
     * ```
     */
    private val typeParameters: List<String>,
    private val returnType: String,
) {
    /**
     * Returns whether [declaration] has this signature.
     */
    fun hasTheSameSignature(declaration: FirCallableDeclaration): Boolean {
        val function = declaration as? FirFunction
        if ((receiverType == null) != (declaration.receiverParameter == null)) return false
        if (contextParameterTypes.size != declaration.contextParameters.size) return false
        if (typeParameters.size != declaration.typeParameters.size) return false
        if (valueParameterTypes?.size != function?.valueParameters?.size) return false

        declaration.lazyResolveToPhase(FirResolvePhase.TYPES)
        if (receiverType != declaration.receiverParameter?.typeRef?.renderType()) return false

        for ([index, contextParameter] in declaration.contextParameters.withIndex()) {
            if (contextParameterTypes[index] != contextParameter.returnTypeRef.renderType()) return false
        }

        if (function != null) {
            requireNotNull(valueParameterTypes)
            for ([index, valueParameter] in function.valueParameters.withIndex()) {
                if (valueParameterTypes[index] != valueParameter.returnTypeRef.renderType()) return false
            }
        }

        for ([index, typeParameter] in declaration.typeParameters.withIndex()) {
            if (typeParameters[index] != typeParameter.renderTypeParameter()) return false
        }

        return returnType == declaration.symbol.resolvedReturnTypeRef.renderType()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FirCallableSignature) return false

        if (receiverType != other.receiverType) return false
        if (contextParameterTypes != other.contextParameterTypes) return false
        if (valueParameterTypes != other.valueParameterTypes) return false
        if (typeParameters != other.typeParameters) return false
        return returnType == other.returnType
    }

    override fun hashCode(): Int {
        var result = receiverType.hashCode()
        result = 31 * result + contextParameterTypes.hashCode()
        result = 31 * result + valueParameterTypes.hashCode()
        result = 31 * result + typeParameters.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }

    @KaImplementationDetail
    companion object {
        fun createSignature(callableSymbol: FirCallableSymbol<*>): FirCallableSignature {
            val declaration = callableSymbol.fir
            declaration.lazyResolveToPhase(FirResolvePhase.TYPES)

            return FirCallableSignature(
                receiverType = declaration.receiverParameter?.typeRef?.renderType(),
                contextParameterTypes = declaration.contextParameters.map { it.returnTypeRef.renderType() },
                valueParameterTypes = (declaration as? FirFunction)?.valueParameters?.map { it.returnTypeRef.renderType() },
                typeParameters = declaration.typeParameters.map { it.renderTypeParameter() },
                returnType = callableSymbol.resolvedReturnTypeRef.renderType(),
            )
        }
    }
}

private fun FirTypeParameterRef.renderTypeParameter(): String =
    symbol.resolvedBounds.joinToString(prefix = "${symbol.name} : ") { it.renderType() }

private fun FirTypeRef.renderType(): String = FirRenderer(
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
    typeRenderer = ConeTypeRenderer(attributeRenderer = MinimalConeTypeAttributeRenderer),
    callableSignatureRenderer = null,
    errorExpressionRenderer = null,
).renderElementAsString(this)

private object MinimalConeTypeAttributeRenderer : ConeAttributeRenderer() {
    override fun render(attributes: Iterable<ConeAttribute<*>>): String =
        attributes.filter { it.isImportant }.let(ToString::render)

    private val ConeAttribute<*>.isImportant get() = this is AbbreviatedTypeAttribute
}
