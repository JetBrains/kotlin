/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KtStarProjectionTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.components.KtClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.components.KtTypeCreator
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.types.KtFe10ClassErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.types.KtFe10UsualClassType
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal class KtFe10TypeCreator(
    override val analysisSession: KtFe10AnalysisSession
) : KtTypeCreator(), Fe10KtAnalysisSessionComponent {
    override val token: ValidityToken
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
            val kotlinType = ErrorUtils.createErrorType("Cannot build class type, descriptor not found for builder $builder")
            return KtFe10ClassErrorType(kotlinType as ErrorType, analysisContext)
        }

        val typeParameters = descriptor.typeConstructor.parameters
        val type = if (typeParameters.size == builder.arguments.size) {
            val projections = builder.arguments.mapIndexed { index, arg ->
                when (arg) {
                    is KtStarProjectionTypeArgument -> StarProjectionImpl(typeParameters[index])
                    is KtTypeArgumentWithVariance -> TypeProjectionImpl(arg.variance, (arg.type as KtFe10Type).type)
                }
            }

            TypeUtils.substituteProjectionsForParameters(descriptor, projections)
        } else {
            descriptor.defaultType
        }

        val typeWithNullability = TypeUtils.makeNullableAsSpecified(type, builder.nullability == KtTypeNullability.NULLABLE)
        return KtFe10UsualClassType(typeWithNullability as SimpleType, descriptor, analysisContext)
    }
}