/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types.typeCreation

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.types.KaFe10ClassErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.types.KaFe10UsualClassType
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation.KaBaseClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation.KaBaseTypeCreator
import org.jetbrains.kotlin.analysis.api.impl.base.types.typeCreation.KaBaseTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeParameterTypeBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils

internal class KaFe10TypeCreator(
    analysisSession: KaFe10Session
) : KaBaseTypeCreator<KaFe10Session>(analysisSession) {
    override fun classType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        val descriptor = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId)
        val builder = KaBaseClassTypeBuilder(this).apply(init)

        if (descriptor == null) {
            val name = classId.asString()
            return buildClassErrorType(name)
        }

        return buildClassType(descriptor, builder)
    }

    override fun classType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        val descriptor = getSymbolDescriptor(symbol) as? ClassDescriptor
        val builder = KaBaseClassTypeBuilder(this).apply(init)

        if (descriptor == null) {
            val name = symbol.classId?.asString() ?: symbol.nameOrAnonymous.asString()
            return buildClassErrorType(name)
        }

        return buildClassType(descriptor, builder)
    }

    private fun buildClassErrorType(name: String): KaClassErrorType {
        val kotlinType =
            ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_CLASS_TYPE, name)

        return KaFe10ClassErrorType(kotlinType, analysisContext)
    }

    private fun buildClassType(descriptor: ClassDescriptor, builder: KaBaseClassTypeBuilder): KaClassType {
        val typeParameters = descriptor.typeConstructor.parameters
        val providedTypeArguments = builder.typeArguments
        val projections = typeParameters.mapIndexed { index, typeParameter ->
            when (val argument = providedTypeArguments.getOrNull(index)) {
                is KaStarTypeProjection, null -> StarProjectionImpl(typeParameter)
                is KaTypeArgumentWithVariance -> TypeProjectionImpl(argument.variance, (argument.type as KaFe10Type).fe10Type)
            }
        }

        val type = TypeUtils.substituteProjectionsForParameters(descriptor, projections)


        val typeWithNullability = TypeUtils.makeNullableAsSpecified(type, builder.isMarkedNullable)
        return KaFe10UsualClassType(typeWithNullability as SimpleType, descriptor, analysisContext)
    }

    override fun typeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType =
        withValidityAssertion {
            val builder = KaBaseTypeParameterTypeBuilder(this).apply(init)
            val descriptor = getSymbolDescriptor(symbol) as? TypeParameterDescriptor
            val kotlinType = descriptor?.defaultType
                ?: ErrorUtils.createErrorType(ErrorTypeKind.NOT_FOUND_DESCRIPTOR_FOR_TYPE_PARAMETER, builder.toString())
            val typeWithNullability = TypeUtils.makeNullableAsSpecified(kotlinType, builder.isMarkedNullable)
            return typeWithNullability.toKtType(analysisContext) as KaTypeParameterType
        }

    private val analysisContext: Fe10AnalysisContext
        get() = analysisSession.analysisContext
}