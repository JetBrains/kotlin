/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.types

import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.resolve.inference.receiverType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal interface KtFirType : ValidityTokenOwner {
    val coneType: ConeKotlinType
}

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

    override val nullability: KtTypeNullability get() = withValidityAssertion { KtTypeNullability.create(coneType.isNullable) }
    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
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

    override val nullability: KtTypeNullability get() = withValidityAssertion { KtTypeNullability.create(coneType.isNullable) }

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
            coneType.receiverType(firBuilder.rootSession) != null
        }

    override val parameterTypes: List<KtType> by cached {
        val parameterTypeArgs = if (coneType.isExtensionFunctionType) typeArguments.subList(1, typeArguments.lastIndex)
        else typeArguments.subList(0, typeArguments.lastIndex)
        parameterTypeArgs.map { (it as KtTypeArgumentWithVariance).type }
    }

    override val returnType: KtType
        get() = withValidityAssertion { (typeArguments.last() as KtTypeArgumentWithVariance).type }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
}

internal class KtFirClassErrorType(
    _coneType: ConeClassErrorType,
    override val token: ValidityToken,
) : KtClassErrorType(), KtFirType {
    override val coneType by weakRef(_coneType)

    override val error: String get() = withValidityAssertion { coneType.diagnostic.reason }
    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
}

internal class KtFirCapturedType(
    _coneType: ConeCapturedType,
    override val token: ValidityToken,
) : KtCapturedType(), KtFirType {
    override val coneType by weakRef(_coneType)
    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
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

    override val nullability: KtTypeNullability get() = withValidityAssertion { KtTypeNullability.create(coneType.isNullable) }
    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
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
    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
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

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
}
