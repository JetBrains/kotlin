/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.types

import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext

internal interface KtFirType : KtType, ValidityTokenOwner {
    val coneType: ConeKotlinType
    val typeCheckerContext: ConeTypeCheckerContext

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }

    override fun isEqualTo(other: KtType): Boolean = withValidityAssertion {
        other.assertIsValid()
        check(other is KtFirType)
        return AbstractTypeChecker.equalTypes(
            typeCheckerContext as AbstractTypeCheckerContext,
            coneType,
            other.coneType
        )
    }

    override fun isSubTypeOf(superType: KtType): Boolean = withValidityAssertion {
        superType.assertIsValid()
        check(superType is KtFirType)
        return AbstractTypeChecker.isSubtypeOf(
            typeCheckerContext as AbstractTypeCheckerContext,
            coneType,
            superType.coneType
        )
    }

    override val isBuiltInFunctionalType: Boolean
        get() = coneType.isBuiltinFunctionalType(typeCheckerContext.session)
}

internal class KtFirClassType(
    coneType: ConeClassLikeTypeImpl,
    typeCheckerContext: ConeTypeCheckerContext,
    override val token: ValidityToken,
    private val firBuilder: KtSymbolByFirBuilder,
) : KtClassType(), KtFirType {
    override val coneType by weakRef(coneType)
    override val typeCheckerContext by weakRef(typeCheckerContext)

    override val classId: ClassId get() = withValidityAssertion { coneType.lookupTag.classId }
    override val classSymbol: KtClassLikeSymbol by cached {
        firBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag) ?: error("Class ${coneType.lookupTag} was not found")
    }
    override val typeArguments: List<KtTypeArgument> by cached {
        coneType.typeArguments.map { typeArgument ->
            firBuilder.buildTypeArgument(typeArgument)
        }
    }
    override val nullability: KtTypeNullability get() = withValidityAssertion { KtTypeNullability.create(coneType.isNullable) }

    override fun asString(): String = withValidityAssertion {
        coneType.render() //todo
    }
}

internal class KtFirErrorType(
    coneType: ConeClassErrorType,
    typeCheckerContext: ConeTypeCheckerContext,
    override val token: ValidityToken,
) : KtErrorType(), KtFirType {
    override val coneType by weakRef(coneType)
    override val typeCheckerContext by weakRef(typeCheckerContext)

    override val error: String get() = withValidityAssertion { coneType.diagnostic.reason }
}

internal class KtFirTypeParameterType(
    coneType: ConeTypeParameterType,
    typeCheckerContext: ConeTypeCheckerContext,
    override val token: ValidityToken,
    private val firBuilder: KtSymbolByFirBuilder,
) : KtTypeParameterType(), KtFirType {
    override val coneType by weakRef(coneType)
    override val typeCheckerContext by weakRef(typeCheckerContext)

    override val name: Name get() = withValidityAssertion { coneType.lookupTag.name }
    override val symbol: KtTypeParameterSymbol by cached {
        firBuilder.buildTypeParameterSymbolByLookupTag(coneType.lookupTag)
            ?: error("Type parameter ${coneType.lookupTag} was not found")
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { KtTypeNullability.create(coneType.isNullable) }

    override fun asString(): String = withValidityAssertion {
        coneType.render() //todo
    }
}

internal class KtFirFlexibleType(
    coneType: ConeFlexibleType,
    typeCheckerContext: ConeTypeCheckerContext,
    override val token: ValidityToken,
    private val firBuilder: KtSymbolByFirBuilder,
) : KtFlexibleType(), KtFirType {
    override val coneType by weakRef(coneType)
    override val typeCheckerContext by weakRef(typeCheckerContext)

    override val lowerBound: KtType by cached { firBuilder.buildKtType(coneType.lowerBound) }
    override val upperBound: KtType by cached { firBuilder.buildKtType(coneType.upperBound) }
}

internal class KtFirIntersectionType(
    coneType: ConeIntersectionType,
    typeCheckerContext: ConeTypeCheckerContext,
    override val token: ValidityToken,
    private val firBuilder: KtSymbolByFirBuilder,
) : KtIntersectionType(), KtFirType {
    override val coneType by weakRef(coneType)
    override val typeCheckerContext by weakRef(typeCheckerContext)

    override val conjuncts: List<KtType> by cached {
        coneType.intersectedTypes.map { conjunct -> firBuilder.buildKtType(conjunct) }
    }
}

internal class KtFirTypeArgumentWithVariance(
    override val type: KtType,
    override val variance: KtTypeArgumentVariance
) : KtTypeArgumentWithVariance()