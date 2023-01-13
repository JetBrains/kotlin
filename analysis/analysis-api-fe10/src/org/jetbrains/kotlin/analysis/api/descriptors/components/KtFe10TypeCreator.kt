/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.components.KtClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.components.KtTypeCreator
import org.jetbrains.kotlin.analysis.api.components.KtTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.types.KtFe10ClassErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.types.KtFe10UsualClassType
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KtTypeParameterType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal class KtFe10TypeCreator(
    override val analysisSession: KtFe10AnalysisSession
) : KtTypeCreator(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun buildClassType(builder: KtClassTypeBuilder): KtClassType {
        val descriptor: ClassDescriptor? = when (builder) {
            is KtClassTypeBuilder.ByClassId -> {
                val fqName = builder.classId.asSingleFqName()
                analysisContext.resolveSession
                    .getTopLevelClassifierDescriptors(fqName, NoLookupLocation.FROM_IDE)
                    .firstIsInstanceOrNull()
            }
            is KtClassTypeBuilder.BySymbol -> {
                getSymbolDescriptor(builder.symbol) as? ClassDescriptor
            }
        }

        if (descriptor == null) {
            val name = when (builder) {
                is KtClassTypeBuilder.ByClassId -> builder.classId.asString()
                is KtClassTypeBuilder.BySymbol ->
                    builder.symbol.classIdIfNonLocal?.asString()
                        ?: builder.symbol.name?.asString()
                        ?: SpecialNames.ANONYMOUS_STRING
            }
            val kotlinType = ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_CLASS_TYPE, name)
            return KtFe10ClassErrorType(kotlinType, analysisContext)
        }

        val typeParameters = descriptor.typeConstructor.parameters
        val type = if (typeParameters.size == builder.arguments.size) {
            val projections = builder.arguments.mapIndexed { index, arg ->
                when (arg) {
                    is KtStarTypeProjection -> StarProjectionImpl(typeParameters[index])
                    is KtTypeArgumentWithVariance -> TypeProjectionImpl(arg.variance, (arg.type as KtFe10Type).fe10Type)
                }
            }

            TypeUtils.substituteProjectionsForParameters(descriptor, projections)
        } else {
            descriptor.defaultType
        }

        val typeWithNullability = TypeUtils.makeNullableAsSpecified(type, builder.nullability == KtTypeNullability.NULLABLE)
        return KtFe10UsualClassType(typeWithNullability as SimpleType, descriptor, analysisContext)
    }

    override fun buildTypeParameterType(builder: KtTypeParameterTypeBuilder): KtTypeParameterType {
        val descriptor = when (builder) {
            is KtTypeParameterTypeBuilder.BySymbol -> {
                getSymbolDescriptor(builder.symbol) as? TypeParameterDescriptor
            }
        }
        val kotlinType = descriptor?.defaultType
            ?: ErrorUtils.createErrorType(ErrorTypeKind.NOT_FOUND_DESCRIPTOR_FOR_TYPE_PARAMETER, builder.toString())
        return kotlinType.toKtType(analysisContext) as KtTypeParameterType
    }
}
