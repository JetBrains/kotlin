/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.types.typeCreation.KaFirTypeCreator
import org.jetbrains.kotlin.analysis.api.fir.utils.coneType
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsTypeCreatorProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaFunctionTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeParameterTypeBuilder
import org.jetbrains.kotlin.fir.types.ConeClassLikeType

internal class KaFirTypeCreatorProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaFirSessionComponent(), KaInternalsTypeCreatorProvider {
    val backingTypeCreator by lazy { KaFirTypeCreator(analysisSession) }

    override val typeCreator: KaTypeCreator
        get() = withValidityAssertion {
            backingTypeCreator
        }

    @KaExperimentalApi
    override fun <T : KaClassType> copy(type: T, init: KaClassTypeBuilder.() -> Unit): KaClassType = withValidityAssertion {
        with(analysisSession) {
            val sourceSymbol = type.symbol
            val sourceIsMarkedNullable = type.isMarkedNullable
            val sourceAnnotationClassIds = type.annotations.classIds.toList()
            val sourceQualifiers = type.qualifiers
            val ownTypeArguments = sourceQualifiers.last().typeArguments
            val outerQualifiers = sourceQualifiers.dropLast(1)

            // The arguments of the type parameters captured by a local class follow the arguments shown in the qualifiers
            val sourceCapturedTypeArguments = (type.coneType as? ConeClassLikeType)?.typeArguments.orEmpty()
                .drop(sourceQualifiers.sumOf { it.typeArguments.size })

            backingTypeCreator.classType(sourceSymbol, sourceCapturedTypeArguments) {
                isMarkedNullable = sourceIsMarkedNullable
                typeArguments { ownTypeArguments }
                outerClasses { index, _ ->
                    typeArguments { outerQualifiers.getOrNull(index)?.typeArguments.orEmpty() }
                }
                annotations(sourceAnnotationClassIds)
                init()
            } as KaClassType
        }
    }

    @KaExperimentalApi
    override fun copy(type: KaUsualClassType, init: KaClassTypeBuilder.() -> Unit): KaUsualClassType = withValidityAssertion {
        copy(type as KaClassType, init) as KaUsualClassType
    }

    @KaExperimentalApi
    override fun copy(type: KaFunctionType, init: KaFunctionTypeBuilder.() -> Unit): KaFunctionType = withValidityAssertion {
        with(analysisSession) {
            val sourceIsMarkedNullable = type.isMarkedNullable
            val sourceIsSuspend = type.isSuspend
            val sourceIsReflectType = type.isReflectType
            val sourceContextParameterTypes = type.contextParameterTypes
            val sourceReceiverType = type.receiverType
            val sourceParameters = type.parameters
            val sourceReturnType = type.returnType
            val sourceAnnotationClassIds = type.annotations.classIds.toList()

            typeCreator.functionType {
                isMarkedNullable = sourceIsMarkedNullable
                isSuspend = sourceIsSuspend
                isReflectType = sourceIsReflectType
                for (contextParameterType in sourceContextParameterTypes) {
                    contextParameter(contextParameterType)
                }
                receiverType = sourceReceiverType
                for (parameter in sourceParameters) {
                    valueParameter(parameter.name, parameter.type)
                }
                returnType = sourceReturnType
                annotations(sourceAnnotationClassIds)
                init()
            }
        }
    }

    @KaExperimentalApi
    override fun copy(type: KaTypeParameterType, init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType = withValidityAssertion {
        with(analysisSession) {
            val sourceSymbol = type.symbol
            val sourceIsMarkedNullable = type.isMarkedNullable
            val sourceAnnotationClassIds = type.annotations.classIds.toList()

            typeCreator.typeParameterType(sourceSymbol) {
                isMarkedNullable = sourceIsMarkedNullable
                annotations(sourceAnnotationClassIds)
                init()
            }
        }
    }
}
