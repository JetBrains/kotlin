/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(SymbolInternals::class)

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.getEffectiveVariance

fun FirDeclarationWithContext<FirCallableDeclaration>.encodeSignature(): String {
    val sig = StringBuilder()

    val typeParameterNames = nameTypeParameters()
    val currentParameters = declaration.typeParameters.mapNotNull { it as? FirTypeParameter }
    val usedTypeParameters = currentParameters.toMutableSet()
    val typeParameterNamer = { typeParameter: FirTypeParameter ->
        usedTypeParameters += typeParameter // TODO: was .original
        typeParameterNames[typeParameter]
            ?: error("$typeParameter is not found when encode the signature of $declaration.")
    }

    val contextReceiverParameters = declaration.contextReceivers
    if (contextReceiverParameters.isNotEmpty()) {
        for (contextReceiverParameter in contextReceiverParameters) {
            sig.encodeForSignature(contextReceiverParameter.typeRef.coneType, session, typeParameterNamer).append(',')
        }
        sig.append('\\')
    }

    val receiverParameter = declaration.receiverTypeRef
    if (receiverParameter != null) {
        sig.encodeForSignature(receiverParameter.coneType, session, typeParameterNamer).append('/')
    }

    if (declaration is FirFunction) {
        for (index in declaration.valueParameters.indices) {
            if (index > 0) {
                sig.append(",")
            }
            if (declaration.valueParameters[index].isVararg) {
                sig.append("*")
            }
            sig.encodeForSignature(declaration.valueParameters[index].returnTypeRef.coneType, session, typeParameterNamer)
        }
    }

    var first = true
    for (typeParameter in typeParameterNames.keys.asSequence().filter { it in usedTypeParameters }) {
        val upperBounds = typeParameter.bounds.filter { !it.isNullableAny }
        if (upperBounds.isEmpty() && typeParameter !in currentParameters) continue

        sig.append(if (first) "|" else ",").append(typeParameterNames[typeParameter])
        first = false
        if (upperBounds.isEmpty()) continue

        sig.append("<:")
        for ((boundIndex, upperBound) in upperBounds.withIndex()) {
            if (boundIndex > 0) {
                sig.append("&")
            }
            sig.encodeForSignature(upperBound.coneType, session, typeParameterNamer)
        }
    }

    return sig.toString()
}

val FirDeclaration.singleFqName
    get() = when (this) {
        is FirCallableDeclaration -> symbol.callableId.asSingleFqName()
        is FirClassLikeDeclaration -> symbol.classId.asSingleFqName()
        else -> error("This declaration has no FqName")
    }

private fun StringBuilder.encodeForSignature(
    type: ConeKotlinType,
    session: FirSession,
    typeParameterNamer: (FirTypeParameter) -> String
): StringBuilder = apply {
    val typeToUse = if (type is ConeDynamicType) {
        session.builtinTypes.anyType.type
    } else {
        type
    }

    val declaration = typeToUse.toSymbol(session)?.fir
        ?: error("Couldn't resolve the type declaration")

    if (declaration is FirTypeParameter) {
        return append(typeParameterNamer(declaration))
    }

    val fqName = declaration.singleFqName.asString()

    if (typeToUse.isSuspendFunctionType(session)) {
        append(fqName.replace("kotlin.coroutines.SuspendFunction", "kotlin.SuspendFunction"))
    } else {
        append(fqName)
    }

    val parameters = (declaration as? FirTypeParameterRefsOwner)?.typeParameters
        ?.mapNotNull { it as? FirTypeParameter }
        ?: error("Expected a declaration with type parameters")

    if (typeToUse.typeArguments.isNotEmpty() && parameters.isNotEmpty()) {
        append("<")
        encodeForSignature(typeToUse.typeArguments[0], parameters[0], session, typeParameterNamer)
        val count = minOf(typeToUse.typeArguments.size, parameters.size)

        for (it in 1 until count) {
            append(",")
            encodeForSignature(typeToUse.typeArguments[it], parameters[it], session, typeParameterNamer)
        }

        append(">")
    }

    if (typeToUse.isMarkedNullable) {
        append("?")
    }
}

private fun StringBuilder.encodeForSignature(
    projection: ConeTypeProjection,
    parameter: FirTypeParameter,
    session: FirSession,
    typeParameterNamer: (FirTypeParameter) -> String
): StringBuilder = apply {
    if (projection is ConeStarProjection) {
        append("*")
    } else {
        when (getEffectiveVariance(parameter.variance, projection.kind.variance)) {
            Variance.IN_VARIANCE -> append("-")
            Variance.OUT_VARIANCE -> append("+")
            Variance.INVARIANT -> {}
        }
        projection.type?.let {
            encodeForSignature(it, session, typeParameterNamer)
        }
    }
}

private val ProjectionKind.variance
    get() = when (this) {
        ProjectionKind.OUT -> Variance.OUT_VARIANCE
        ProjectionKind.IN -> Variance.IN_VARIANCE
        ProjectionKind.INVARIANT -> Variance.INVARIANT
        else -> error("Couldn't deduce variance")
    }

private fun FirDeclarationWithContext<*>.nameTypeParameters(): Map<FirTypeParameter, String> {
    val result = mutableMapOf<FirTypeParameter, String>()

    for ((listIndex, list) in collectTypeParameters().withIndex()) {
        for ((indexInList, typeParameter) in list.withIndex()) {
            result[typeParameter] = "$listIndex:$indexInList"
        }
    }

    return result
}

private fun FirDeclarationWithContext<*>.collectTypeParameters(): List<List<FirTypeParameter>> {
    val result = mutableListOf<List<FirTypeParameter>>()
    var current: FirDeclarationWithContext<*>? = this

    while (current != null) {
        getOwnTypeParameters(current.declaration)?.let { result += it }

        if (current.declaration is FirConstructor) {
            current = current.parent
        }

        current = current?.parent
    }

    return result
}

private fun getOwnTypeParameters(declaration: FirDeclaration): List<FirTypeParameter>? =
    when (declaration) {
        is FirClass -> declaration.typeParameters.mapNotNull { it as? FirTypeParameter }
        is FirPropertyAccessor -> when (val property = declaration.propertySymbol?.fir) {
            null -> emptyList()
            else -> getOwnTypeParameters(property)
        }
        is FirCallableDeclaration -> declaration.typeParameters.mapNotNull { it as? FirTypeParameter }
        else -> null
    }
