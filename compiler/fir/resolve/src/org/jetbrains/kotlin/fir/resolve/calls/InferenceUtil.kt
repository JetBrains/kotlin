/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintIncorporator
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver
import org.jetbrains.kotlin.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContextDelegate
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.model.TypeVariableTypeConstructorMarker

fun ConeTypeContext.hasNullableSuperType(type: ConeKotlinType): Boolean {
    if (type is ConeClassLikeType) return false

    if (type !is ConeLookupTagBasedType) return false // TODO?
    val symbol = type.lookupTag.toSymbol(session) ?: return false // TODO?!
    for (superType in symbol.supertypes()) {
        if (superType.isNullableType()) return true
    }
//
//    for (KotlinType supertype : getImmediateSupertypes(type)) {
//        if (isNullableType(supertype)) return true;
//    }

    return false
}

class ConeTypeVariableTypeConstructor(val debugName: String) : ConeSymbol, ConeClassifierLookupTag(), TypeVariableTypeConstructorMarker {
    override val name: Name get() = Name.identifier(debugName)
}

class TypeParameterBasedTypeVariable(val typeParameterSymbol: FirTypeParameterSymbol) :
    ConeTypeVariable(typeParameterSymbol.name.identifier)

open class ConeTypeVariable(name: String) : TypeVariableMarker {
    val typeConstructor = ConeTypeVariableTypeConstructor(name)
    val defaultType = ConeTypeVariableType(ConeNullability.NOT_NULL, typeConstructor)
}

class InferenceComponents(
    val ctx: TypeSystemInferenceExtensionContextDelegate,
    val session: FirSession,
    val returnTypeCalculator: ReturnTypeCalculator,
    val scopeSession: ScopeSession
) {
    private val approximator = object : AbstractTypeApproximator(ctx) {
        override fun createErrorType(message: String): SimpleTypeMarker {
            return ConeClassErrorType(message)
        }
    }
    val trivialConstraintTypeInferenceOracle = TrivialConstraintTypeInferenceOracle(ctx)
    private val incorporator = ConstraintIncorporator(approximator, trivialConstraintTypeInferenceOracle)
    private val injector = ConstraintInjector(incorporator, approximator)
    val resultTypeResolver = ResultTypeResolver(approximator, trivialConstraintTypeInferenceOracle)

    fun createConstraintSystem(): NewConstraintSystemImpl {
        return NewConstraintSystemImpl(injector, ctx)
    }

}

