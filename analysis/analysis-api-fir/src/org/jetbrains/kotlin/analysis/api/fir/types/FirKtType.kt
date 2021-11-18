/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KtTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.fir.types.isSuspendFunctionType
import org.jetbrains.kotlin.fir.types.receiverType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal interface KtFirType : ValidityTokenOwner {
    val coneType: ConeKotlinType
}

private fun KtFirType.typeEquals(other: Any?): Boolean {
    if (other !is KtFirType) return false
    return this.coneType == other.coneType
}

private fun KtFirType.typeHashcode(): Int = coneType.hashCode()

internal class KtFirUsualClassType(
    override val coneType: ConeClassLikeTypeImpl,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
) : KtUsualClassType(), KtFirType {
    override val classId: ClassId get() = withValidityAssertion { coneType.lookupTag.classId }
    override val classSymbol: KtClassLikeSymbol by cached {
        builder.classifierBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag)
            ?: error("Class ${coneType.lookupTag} was not found")
    }
    override val typeArguments: List<KtTypeArgument> by cached {
        coneType.typeArguments.map { typeArgument ->
            builder.typeBuilder.buildTypeArgument(typeArgument)
        }
    }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder.rootSession, token)
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }
    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirFunctionalType(
    override val coneType: ConeClassLikeTypeImpl,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
) : KtFunctionalType(), KtFirType {
    override val classId: ClassId get() = withValidityAssertion { coneType.lookupTag.classId }
    override val classSymbol: KtClassLikeSymbol by cached {
        builder.classifierBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag)
            ?: error("Class ${coneType.lookupTag} was not found")
    }
    override val typeArguments: List<KtTypeArgument> by cached {
        coneType.typeArguments.map { typeArgument ->
            builder.typeBuilder.buildTypeArgument(typeArgument)
        }
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
            if (coneType.isExtensionFunctionType) (typeArguments.first() as KtTypeArgumentWithVariance).type
            else null
        }

    override val hasReceiver: Boolean
        get() = withValidityAssertion {
            coneType.receiverType(builder.rootSession) != null
        }

    override val parameterTypes: List<KtType> by cached {
        val parameterTypeArgs = if (coneType.isExtensionFunctionType) typeArguments.subList(1, typeArguments.lastIndex)
        else typeArguments.subList(0, typeArguments.lastIndex)
        parameterTypeArgs.map { (it as KtTypeArgumentWithVariance).type }
    }

    override val returnType: KtType
        get() = withValidityAssertion { (typeArguments.last() as KtTypeArgumentWithVariance).type }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirClassErrorType(
    override val coneType: ConeClassErrorType,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
) : KtClassErrorType(), KtFirType {

    override val error: String get() = withValidityAssertion { coneType.diagnostic.reason }
    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder.rootSession, token)
    }

    override val candidateClassSymbols: Collection<KtClassLikeSymbol> by cached {
        val symbols = coneType.diagnostic.getCandidateSymbols().filterIsInstance<FirClassLikeSymbol<*>>()
        symbols.map { builder.classifierBuilder.buildClassLikeSymbol(it.fir) }
    }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirCapturedType(
    override val coneType: ConeCapturedType,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
) : KtCapturedType(), KtFirType {
    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder.rootSession, token)
    }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }


    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirDefinitelyNotNullType(
    override val coneType: ConeDefinitelyNotNullType,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
) : KtDefinitelyNotNullType(), KtFirType {
    override val original: KtType by cached { builder.typeBuilder.buildKtType(this.coneType.original) }
    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder.rootSession, token)
    }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirTypeParameterType(
    override val coneType: ConeTypeParameterType,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
) : KtTypeParameterType(), KtFirType {
    override val name: Name get() = withValidityAssertion { coneType.lookupTag.name }
    override val symbol: KtTypeParameterSymbol by cached {
        builder.classifierBuilder.buildTypeParameterSymbolByLookupTag(coneType.lookupTag)
            ?: error("Type parameter ${coneType.lookupTag} was not found")
    }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder.rootSession, token)
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirFlexibleType(
    override val coneType: ConeFlexibleType,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
) : KtFlexibleType(), KtFirType {

    override val lowerBound: KtType by cached { builder.typeBuilder.buildKtType(coneType.lowerBound) }
    override val upperBound: KtType by cached { builder.typeBuilder.buildKtType(coneType.upperBound) }
    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder.rootSession, token)
    }
    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirIntersectionType(
    override val coneType: ConeIntersectionType,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
) : KtIntersectionType(), KtFirType {
    override val conjuncts: List<KtType> by cached {
        coneType.intersectedTypes.map { conjunct -> builder.typeBuilder.buildKtType(conjunct) }
    }
    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder.rootSession, token)
    }
    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirIntegerLiteralType(
    override val coneType: ConeIntegerLiteralType,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
) : KtIntegerLiteralType(), KtFirType {
    override val isUnsigned: Boolean get() = withValidityAssertion { coneType.isUnsigned }

    override val value: Long get() = withValidityAssertion { coneType.value }

    override val possibleTypes: List<KtClassType> by cached {
        coneType.possibleTypes.map { possibleType ->
            builder.typeBuilder.buildKtType(possibleType) as KtClassType
        }
    }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder.rootSession, token)
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

private fun ConeNullability.asKtNullability(): KtTypeNullability = when (this) {
    ConeNullability.NULLABLE -> KtTypeNullability.NULLABLE
    ConeNullability.UNKNOWN -> KtTypeNullability.UNKNOWN
    ConeNullability.NOT_NULL -> KtTypeNullability.NON_NULLABLE
}
