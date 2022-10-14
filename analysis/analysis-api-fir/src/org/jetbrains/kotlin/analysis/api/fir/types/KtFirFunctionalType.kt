/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.UsualClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isExtensionFunctionType
import org.jetbrains.kotlin.fir.types.isSuspendFunctionType
import org.jetbrains.kotlin.fir.types.receiverType
import org.jetbrains.kotlin.fir.types.renderForDebugging
import org.jetbrains.kotlin.name.ClassId

internal class KtFirFunctionalType(
    override val coneType: ConeClassLikeTypeImpl,
    override val token: KtLifetimeToken,
    private val builder: KtSymbolByFirBuilder,
) : KtFunctionalType(), KtFirType {
    override val classId: ClassId get() = withValidityAssertion { coneType.lookupTag.classId }
    override val classSymbol: KtClassLikeSymbol by cached {
        builder.classifierBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag)
            ?: errorWithFirSpecificEntries("Class was not found", coneType = coneType)
    }
    override val ownTypeArguments: List<KtTypeProjection> get() = withValidityAssertion { qualifiers.last().typeArguments }

    override val qualifiers: List<KtClassTypeQualifier.KtResolvedClassTypeQualifier> by cached {
        UsualClassTypeQualifierBuilder.buildQualifiers(coneType, builder)
    }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder.rootSession, token)
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override val isSuspend: Boolean get() = withValidityAssertion { coneType.isSuspendFunctionType(builder.rootSession) }
    override val arity: Int
        get() = withValidityAssertion {
            if (coneType.isExtensionFunctionType) coneType.typeArguments.size - 2
            else coneType.typeArguments.size - 1
        }

    override val receiverType: KtType?
        get() = withValidityAssertion {
            if (coneType.isExtensionFunctionType) (ownTypeArguments.first() as KtTypeArgumentWithVariance).type
            else null
        }

    override val hasReceiver: Boolean
        get() = withValidityAssertion {
            coneType.receiverType(builder.rootSession) != null
        }

    override val parameterTypes: List<KtType> by cached {
        val parameterTypeArgs = if (coneType.isExtensionFunctionType) ownTypeArguments.subList(1, ownTypeArguments.lastIndex)
        else ownTypeArguments.subList(0, ownTypeArguments.lastIndex)
        parameterTypeArgs.map { (it as KtTypeArgumentWithVariance).type }
    }

    override val returnType: KtType
        get() = withValidityAssertion { (ownTypeArguments.last() as KtTypeArgumentWithVariance).type }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.renderForDebugging() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}
