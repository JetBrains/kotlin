/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.name.Name

class QualifierPartBuilder(internal val destination: MutableList<FirQualifierPart>) {
    fun part(name: Name, argumentsBuilder: TypeArgumentsBuilder.() -> Unit = {}) {
        val typeArgumentList = FirTypeArgumentListImpl(source = null)
        TypeArgumentsBuilder(typeArgumentList).argumentsBuilder()
        destination += FirQualifierPartImpl(source = null, name, typeArgumentList)
    }

    class TypeArgumentsBuilder(internal val typeArgumentList: FirTypeArgumentListImpl) {
        fun argument(typeArgument: FirTypeProjection) {
            typeArgumentList.typeArguments += typeArgument
        }
    }
}

fun typeFromQualifierParts(
    isMarkedNullable: Boolean,
    typeResolver: FirSupertypeGenerationExtension.TypeResolveService,
    source: KtSourceElement,
    builder: QualifierPartBuilder.() -> Unit
): ConeKotlinType {
    val userTypeRef = buildUserTypeRef {
        this.isMarkedNullable = isMarkedNullable
        this.source = source
        QualifierPartBuilder(qualifier).builder()
    }
    return typeResolver.resolveUserType(userTypeRef).coneType
}
