/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.UsualClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.KaContextReceiverImpl
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId

internal class KaFirFunctionalType(
    override val coneType: ConeClassLikeTypeImpl,
    private val builder: KaSymbolByFirBuilder,
) : KaFunctionType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token

    override val classId: ClassId get() = withValidityAssertion { coneType.lookupTag.classId }

    override val symbol: KaClassLikeSymbol by cached {
        builder.classifierBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag)
            ?: errorWithFirSpecificEntries("Class was not found", coneType = coneType)
    }
    override val typeArguments: List<KaTypeProjection> get() = withValidityAssertion { qualifiers.last().typeArguments }

    override val qualifiers: List<KaClassTypeQualifier.KaResolvedClassTypeQualifier> by cached {
        UsualClassTypeQualifierBuilder.buildQualifiers(coneType, builder)
    }

    override val annotations: KaAnnotationList by cached {
        KaFirAnnotationListForType.create(coneType, builder)
    }

    override val nullability: KaTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override val abbreviatedType: KaUsualClassType?
        get() = withValidityAssertion { null }

    override val isSuspend: Boolean get() = withValidityAssertion { coneType.isSuspendOrKSuspendFunctionType(builder.rootSession) }

    override val isReflectType: Boolean
        get() = withValidityAssertion { coneType.functionTypeKind(builder.rootSession)?.isReflectType == true }

    override val arity: Int get() = withValidityAssertion { parameterTypes.size }

    @OptIn(KaAnalysisApiInternals::class)
    override val contextReceivers: List<KaContextReceiver> by cached {
        coneType.contextReceiversTypes(builder.rootSession)
            .map {
                // Context receivers in function types may not have labels, hence the `null` label.
                KaContextReceiverImpl(it.buildKtType(), label = null, token)
            }
    }

    override val hasContextReceivers: Boolean get() = withValidityAssertion { contextReceivers.isNotEmpty() }

    override val receiverType: KaType? by cached {
        coneType.receiverType(builder.rootSession)?.buildKtType()
    }

    override val hasReceiver: Boolean get() = withValidityAssertion { receiverType != null }

    override val parameterTypes: List<KaType> by cached {
        coneType.valueParameterTypesWithoutReceivers(builder.rootSession).map { it.buildKtType() }
    }

    override val returnType: KaType by cached {
        coneType.returnType(builder.rootSession).buildKtType()
    }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    private fun ConeKotlinType.buildKtType(): KaType = builder.typeBuilder.buildKtType(this)
}
