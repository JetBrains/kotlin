/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.components.KaTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.types.KaFe10ClassErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.types.KaFe10UsualClassType
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseTypeCreator
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal class KaFe10TypeCreator(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseTypeCreator<KaFe10Session>(), KaFe10SessionComponent {
    override fun buildClassType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        return buildClassType(KaBaseClassTypeBuilder.ByClassId(classId, token).apply(init))
    }

    override fun buildClassType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit): KaType = withValidityAssertion {
        return buildClassType(KaBaseClassTypeBuilder.BySymbol(symbol, token).apply(init))
    }

    private fun buildClassType(builder: KaBaseClassTypeBuilder): KaType {
        val descriptor: ClassDescriptor? = when (builder) {
            is KaBaseClassTypeBuilder.ByClassId -> {
                val fqName = builder.classId.asSingleFqName()
                analysisContext.resolveSession
                    .getTopLevelClassifierDescriptors(fqName, NoLookupLocation.FROM_IDE)
                    .firstIsInstanceOrNull()
            }
            is KaBaseClassTypeBuilder.BySymbol -> {
                getSymbolDescriptor(builder.symbol) as? ClassDescriptor
            }
        }

        if (descriptor == null) {
            val name = when (builder) {
                is KaBaseClassTypeBuilder.ByClassId -> builder.classId.asString()
                is KaBaseClassTypeBuilder.BySymbol ->
                    builder.symbol.classId?.asString()
                        ?: builder.symbol.name?.asString()
                        ?: SpecialNames.ANONYMOUS_STRING
            }
            val kotlinType = ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_CLASS_TYPE, name)
            return KaFe10ClassErrorType(kotlinType, analysisContext)
        }

        val typeParameters = descriptor.typeConstructor.parameters
        val type = if (typeParameters.size == builder.arguments.size) {
            val projections = builder.arguments.mapIndexed { index, arg ->
                when (arg) {
                    is KaStarTypeProjection -> StarProjectionImpl(typeParameters[index])
                    is KaTypeArgumentWithVariance -> TypeProjectionImpl(arg.variance, (arg.type as KaFe10Type).fe10Type)
                }
            }

            TypeUtils.substituteProjectionsForParameters(descriptor, projections)
        } else {
            descriptor.defaultType
        }

        val typeWithNullability = TypeUtils.makeNullableAsSpecified(type, builder.isMarkedNullable)
        return KaFe10UsualClassType(typeWithNullability as SimpleType, descriptor, analysisContext)
    }

    override fun buildTypeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType {
        withValidityAssertion {
            val builder = KaBaseTypeParameterTypeBuilder.BySymbol(symbol, token).apply(init)
            val descriptor = getSymbolDescriptor(builder.symbol) as? TypeParameterDescriptor
            val kotlinType = descriptor?.defaultType
                ?: ErrorUtils.createErrorType(ErrorTypeKind.NOT_FOUND_DESCRIPTOR_FOR_TYPE_PARAMETER, builder.toString())
            val typeWithNullability = TypeUtils.makeNullableAsSpecified(kotlinType, builder.isMarkedNullable)
            return typeWithNullability.toKtType(analysisContext) as KaTypeParameterType
        }
    }
}
