/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.types

import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.resolve.inference.receiverType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.idea.fir.getCandidateSymbols
import org.jetbrains.kotlin.idea.frontend.api.KtTypeArgument
import org.jetbrains.kotlin.idea.frontend.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.*
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal interface KtFirType : ValidityTokenOwner {
    val coneType: ConeKotlinType
}

private fun KtFirType.typeEquals(other: Any?): Boolean {
    if (other !is KtFirType) return false
    if (this.token != other.token) return false
    return this.coneType == other.coneType
}

private fun KtFirType.typeHashcode(): Int = token.hashCode() * 31 + coneType.hashCode()

internal class KtFirUsualClassType(
    _coneType: ConeClassLikeTypeImpl,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder,
) : KtUsualClassType(), KtFirType {
    override val coneType by weakRef(_coneType)
    private val builder by weakRef(_builder)

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

    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }
    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirFunctionalType(
    _coneType: ConeClassLikeTypeImpl,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder,
) : KtFunctionalType(), KtFirType {
    override val coneType by weakRef(_coneType)
    private val builder by weakRef(_builder)

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
    _coneType: ConeClassErrorType,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder,
) : KtClassErrorType(), KtFirType {
    override val coneType by weakRef(_coneType)
    private val builder by weakRef(_builder)

    override val error: String get() = withValidityAssertion { coneType.diagnostic.reason }
    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override val candidateClassSymbols: Collection<KtClassLikeSymbol> by cached {
        val symbols = coneType.diagnostic.getCandidateSymbols().filterIsInstance<FirClassLikeSymbol<*>>()
        symbols.map { builder.classifierBuilder.buildClassLikeSymbol(it.fir) }
    }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirCapturedType(
    _coneType: ConeCapturedType,
    override val token: ValidityToken,
) : KtCapturedType(), KtFirType {
    override val coneType by weakRef(_coneType)
    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }
    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirDefinitelyNotNullType(
    _coneType: ConeDefinitelyNotNullType,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder,
) : KtDefinitelyNotNullType(), KtFirType {
    override val coneType by weakRef(_coneType)
    private val builder by weakRef(_builder)

    override val original: KtType by cached { builder.typeBuilder.buildKtType(this.coneType.original) }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirTypeParameterType(
    _coneType: ConeTypeParameterType,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder,
) : KtTypeParameterType(), KtFirType {
    override val coneType by weakRef(_coneType)
    private val builder by weakRef(_builder)

    override val name: Name get() = withValidityAssertion { coneType.lookupTag.name }
    override val symbol: KtTypeParameterSymbol by cached {
        builder.classifierBuilder.buildTypeParameterSymbolByLookupTag(coneType.lookupTag)
            ?: error("Type parameter ${coneType.lookupTag} was not found")
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirFlexibleType(
    _coneType: ConeFlexibleType,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder,
) : KtFlexibleType(), KtFirType {
    override val coneType by weakRef(_coneType)
    private val builder by weakRef(_builder)

    override val lowerBound: KtType by cached { builder.typeBuilder.buildKtType(coneType.lowerBound) }
    override val upperBound: KtType by cached { builder.typeBuilder.buildKtType(coneType.upperBound) }

    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

internal class KtFirIntersectionType(
    _coneType: ConeIntersectionType,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder,
) : KtIntersectionType(), KtFirType {
    override val coneType by weakRef(_coneType)
    private val builder by weakRef(_builder)

    override val conjuncts: List<KtType> by cached {
        coneType.intersectedTypes.map { conjunct -> builder.typeBuilder.buildKtType(conjunct) }
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