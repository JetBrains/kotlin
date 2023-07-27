/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class TypeApproximatorForMetadataSerializer(session: FirSession) :
    AbstractTypeApproximator(session.typeContext, session.languageVersionSettings) {

    override fun createErrorType(debugName: String): SimpleTypeMarker {
        return ConeErrorType(ConeIntermediateDiagnostic(debugName))
    }
}

fun ConeKotlinType.suspendFunctionTypeToFunctionTypeWithContinuation(session: FirSession, continuationClassId: ClassId): ConeClassLikeType {
    require(this.isSuspendOrKSuspendFunctionType(session))
    val kind =
        if (isReflectFunctionType(session)) FunctionTypeKind.KFunction
        else FunctionTypeKind.Function
    val fullyExpandedType = type.fullyExpandedType(session)
    val typeArguments = fullyExpandedType.typeArguments
    val functionTypeId = ClassId(kind.packageFqName, kind.numberedClassName(typeArguments.size))
    val lastTypeArgument = typeArguments.last()
    return ConeClassLikeTypeImpl(
        functionTypeId.toLookupTag(),
        typeArguments = (typeArguments.dropLast(1) + continuationClassId.toLookupTag().constructClassType(
            arrayOf(lastTypeArgument),
            isNullable = false
        ) + session.builtinTypes.nullableAnyType.type).toTypedArray(),
        isNullable = fullyExpandedType.isNullable,
        attributes = fullyExpandedType.attributes
    )
}

fun FirMemberDeclaration.shouldBeSerialized(actualizedExpectDeclaration: Set<FirDeclaration>?): Boolean {
    return !isExpect || actualizedExpectDeclaration == null || this !in actualizedExpectDeclaration
}
