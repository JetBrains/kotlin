/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.UsualClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.KtContextReceiverImpl
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId

internal class KtFirFunctionalType(
    override val coneType: ConeClassLikeTypeImpl,
    private val builder: KtSymbolByFirBuilder,
) : KtFunctionalType(), KtFirType {
    override val token: KtLifetimeToken get() = builder.token

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

    override val isSuspend: Boolean get() = withValidityAssertion { coneType.isSuspendOrKSuspendFunctionType(builder.rootSession) }

    override val isReflectType: Boolean
        get() = withValidityAssertion { coneType.functionTypeKind(builder.rootSession)?.isReflectType == true }

    override val arity: Int
        get() = withValidityAssertion {
            if (coneType.isExtensionFunctionType) coneType.typeArguments.size - 2
            else coneType.typeArguments.size - 1
        }

    @OptIn(KtAnalysisApiInternals::class)
    override val contextReceivers: List<KtContextReceiver> by cached {
        ownTypeArguments.subList(0, coneType.contextReceiversNumberForFunctionType).map { typeProjection ->
            // Context receivers in function types may not have labels, hence the `null` label.
            KtContextReceiverImpl((typeProjection as KtTypeArgumentWithVariance).type, _label = null, token)
        }
    }

    override val hasContextReceivers: Boolean get() = withValidityAssertion { coneType.hasContextReceivers }

    override val receiverType: KtType?
        get() = withValidityAssertion {
            if (coneType.isExtensionFunctionType) {
                // The extension receiver type follows any context receiver types.
                (ownTypeArguments[coneType.contextReceiversNumberForFunctionType] as KtTypeArgumentWithVariance).type
            } else null
        }

    override val hasReceiver: Boolean
        get() = withValidityAssertion {
            coneType.receiverType(builder.rootSession) != null
        }

    override val parameterTypes: List<KtType> by cached {
        // Parameter types follow context and extension receiver types.
        val firstIndex = coneType.contextReceiversNumberForFunctionType + (if (coneType.isExtensionFunctionType) 1 else 0)
        val parameterTypeArgs = ownTypeArguments.subList(firstIndex, ownTypeArguments.lastIndex)
        parameterTypeArgs.map { (it as KtTypeArgumentWithVariance).type }
    }

    override val returnType: KtType
        get() = withValidityAssertion { (ownTypeArguments.last() as KtTypeArgumentWithVariance).type }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.renderForDebugging() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}
