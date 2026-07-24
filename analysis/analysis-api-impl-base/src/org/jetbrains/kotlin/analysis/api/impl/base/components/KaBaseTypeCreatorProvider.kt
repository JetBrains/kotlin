/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsTypeCreatorProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaFunctionTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeParameterTypeBuilder

@KaImplementationDetail
abstract class KaBaseTypeCreatorProvider<T : KaSession> : KaBaseSessionComponent<T>(), KaInternalsTypeCreatorProvider {
    @KaExperimentalApi
    override fun <T : KaClassType> copy(type: T, init: KaClassTypeBuilder.() -> Unit): KaClassType = with(analysisSession) {
        val sourceSymbol = type.symbol
        val sourceIsMarkedNullable = type.isMarkedNullable
        val sourceAnnotationClassIds = type.annotations.classIds.toList()
        val sourceTypeArguments = type.typeArguments

        typeCreator.classType(sourceSymbol) {
            isMarkedNullable = sourceIsMarkedNullable
            typeArguments { sourceTypeArguments }
            annotations(sourceAnnotationClassIds)
            init()
        } as KaClassType
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
            val sourceContextReceivers = type.contextReceivers
            val sourceReceiverType = type.receiverType
            val sourceParameters = type.parameters
            val sourceReturnType = type.returnType
            val sourceAnnotationClassIds = type.annotations.classIds.toList()

            typeCreator.functionType {
                isMarkedNullable = sourceIsMarkedNullable
                isSuspend = sourceIsSuspend
                isReflectType = sourceIsReflectType
                for (contextReceiver in sourceContextReceivers) {
                    contextParameter(contextReceiver.type)
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
