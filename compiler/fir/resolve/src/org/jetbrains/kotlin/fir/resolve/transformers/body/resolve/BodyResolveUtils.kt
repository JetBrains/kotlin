/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.resolve.calls.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowInferenceContext
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ErrorTypeConstructor
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContextDelegate

inline fun <reified T : FirElement> FirBasedSymbol<*>.firUnsafe(): T {
    val fir = this.fir
    require(fir is T) {
        "Not an expected fir element type = ${T::class}, symbol = ${this}, fir = ${fir.renderWithType()}"
    }
    return fir
}

internal inline var FirExpression.resultType: FirTypeRef
    get() = typeRef
    set(type) {
        replaceTypeRef(type)
    }

internal interface UniversalConeInferenceContext :
    ConeInferenceContext, TypeSystemInferenceExtensionContextDelegate, DataFlowInferenceContext

internal fun FirSession.inferenceContext(): UniversalConeInferenceContext {
    val session = this
    return object : UniversalConeInferenceContext {
        override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? {
            // TODO: implement
            return null
        }

        override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): KotlinTypeMarker {
            TODO("not implemented")
        }

        override val session: FirSession
            get() = session

        override fun KotlinTypeMarker.removeExactAnnotation(): KotlinTypeMarker {
            return this
        }

        override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
            require(this is ErrorTypeConstructor)
            return ConeClassErrorType(reason)
        }
    }
}

internal fun inferenceComponents(
    session: FirSession,
    returnTypeCalculator: ReturnTypeCalculator,
    scopeSession: ScopeSession
): InferenceComponents {
    val inferenceContext = session.inferenceContext()
    return InferenceComponents(inferenceContext, session, returnTypeCalculator, scopeSession)
}