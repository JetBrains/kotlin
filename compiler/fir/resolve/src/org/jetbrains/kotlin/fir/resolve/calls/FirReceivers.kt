/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirSealedClassImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.impl.FirThisReceiverExpressionImpl
import org.jetbrains.kotlin.fir.references.impl.FirImplicitThisReference
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.scopes.scope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.ClassId

interface Receiver {
    fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirScope?
}

interface ReceiverValue : Receiver {
    val type: ConeKotlinType

    val receiverExpression: FirExpression

    override fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? =
        type.scope(useSiteSession, scopeSession)
}

private fun receiverExpression(symbol: AbstractFirBasedSymbol<*>, type: ConeKotlinType): FirExpression =
    FirThisReceiverExpressionImpl(null, FirImplicitThisReference(symbol)).apply {
        typeRef = FirResolvedTypeRefImpl(null, type)
    }

class ClassDispatchReceiverValue(klassSymbol: FirClassSymbol<*>) : ReceiverValue {
    override val type: ConeKotlinType = ConeClassLikeTypeImpl(
        klassSymbol.toLookupTag(),
        (klassSymbol.fir as? FirTypeParametersOwner)?.typeParameters?.map { ConeStarProjection }?.toTypedArray().orEmpty(),
        isNullable = false
    )

    override val receiverExpression: FirExpression = receiverExpression(klassSymbol, type)
}

// TODO: should inherit just Receiver, not ReceiverValue
abstract class AbstractExplicitReceiver<E : FirExpression> : ReceiverValue {
    abstract val explicitReceiver: FirExpression

    override val type: ConeKotlinType
        get() = explicitReceiver.typeRef.coneTypeSafe()
            ?: ConeKotlinErrorType("No type calculated for: ${explicitReceiver.renderWithType()}") // TODO: assert here

    override val receiverExpression: FirExpression
        get() = explicitReceiver
}

fun qualifierOrExpressionReceiver(explicitReceiver: FirExpression): AbstractExplicitReceiver<*> {
    if (explicitReceiver is FirResolvedQualifier) return QualifierReceiver(explicitReceiver)
    return ExpressionReceiverValue(explicitReceiver)
}

class QualifierReceiver(override val explicitReceiver: FirResolvedQualifier) : AbstractExplicitReceiver<FirResolvedQualifier>() {
    private fun getClassSymbolWithCallablesScope(
        classId: ClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): Pair<FirClassSymbol<*>?, FirScope?> {
        val symbol = useSiteSession.firSymbolProvider.getClassLikeSymbolByFqName(classId) ?: return null to null
        if (symbol is FirTypeAliasSymbol) {
            val expansionSymbol = symbol.fir.expandedConeType?.lookupTag?.toSymbol(useSiteSession)
            if (expansionSymbol != null) {
                return getClassSymbolWithCallablesScope(expansionSymbol.classId, useSiteSession, scopeSession)
            }
        } else {
            return (symbol as? FirClassSymbol<*>)?.let { klassSymbol ->
                klassSymbol to when (val klass = klassSymbol.fir) {
                    is FirClassImpl -> {
                        when (klass.classKind) {
                            ClassKind.ENUM_CLASS -> FirStaticScope(declaredMemberScope(klass))
                            ClassKind.OBJECT -> klass.scope(ConeSubstitutor.Empty, useSiteSession, scopeSession)
                            else -> null
                        }
                    }
                    is FirSealedClassImpl -> null
                    else -> /* Java */ FirStaticScope(klass.scope(ConeSubstitutor.Empty, useSiteSession, scopeSession))
                }
            } ?: (null to null)
        }

        return null to null
    }

    override fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? {
        val qualifiedReceiver = explicitReceiver as? FirResolvedQualifier
            ?: return super.scope(useSiteSession, scopeSession)
        val classId = qualifiedReceiver.classId ?: return null

        val (classSymbol, callablesScope) = getClassSymbolWithCallablesScope(classId, useSiteSession, scopeSession)
        if (classSymbol != null) {
            val klass = classSymbol.fir
            val classifierScope = if (klass is FirClassImpl || klass is FirSealedClassImpl) {
                nestedClassifierScope(klass)
            } else {
                useSiteSession.firSymbolProvider.getNestedClassifierScope(classId)
            }

            val qualifierScope = FirQualifierScope(callablesScope, classifierScope)
            if ((klass as? FirRegularClass)?.companionObject == null) {
                return qualifierScope
            }
            val companionScope = super.scope(useSiteSession, scopeSession) ?: return qualifierScope
            return FirCompositeScope(qualifierScope, companionScope)
        }
        return super.scope(useSiteSession, scopeSession)
    }
}

private class ExpressionReceiverValue(
    override val explicitReceiver: FirExpression
) : AbstractExplicitReceiver<FirExpression>(), ReceiverValue

abstract class ImplicitReceiverValue<S : AbstractFirBasedSymbol<*>>(
    val boundSymbol: S,
    type: ConeKotlinType,
    private val useSiteSession: FirSession,
    private val scopeSession: ScopeSession
) : ReceiverValue {
    final override var type: ConeKotlinType = type
        private set

    var implicitScope: FirScope? = type.scope(useSiteSession, scopeSession)
        private set

    override fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? = implicitScope

    override val receiverExpression: FirExpression = receiverExpression(boundSymbol, type)

    /*
     * Should be called only in ImplicitReceiverStack
     */
    internal fun replaceType(type: ConeKotlinType) {
        if (type == this.type) return
        this.type = type
        implicitScope = type.scope(useSiteSession, scopeSession)
    }
}

class ImplicitDispatchReceiverValue(
    boundSymbol: FirClassSymbol<*>,
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession
) : ImplicitReceiverValue<FirClassSymbol<*>>(boundSymbol, type, useSiteSession, scopeSession) {
    val implicitCompanionScopes: List<FirScope> = run {
        val klass = boundSymbol.fir as? FirRegularClass ?: return@run emptyList()
        listOfNotNull(klass.companionObject?.scope(ConeSubstitutor.Empty, useSiteSession, scopeSession)) +
                lookupSuperTypes(klass, lookupInterfaces = false, deep = true, useSiteSession = useSiteSession).mapNotNull {
                    val superClass = (it as? ConeClassLikeType)?.lookupTag?.toSymbol(useSiteSession)?.fir as? FirRegularClass
                    superClass?.companionObject?.scope(ConeSubstitutor.Empty, useSiteSession, scopeSession)
                }
    }
}

class ImplicitExtensionReceiverValue(
    boundSymbol: FirCallableSymbol<*>,
    type: ConeKotlinType,
    useSiteSession: FirSession,
    scopeSession: ScopeSession
) : ImplicitReceiverValue<FirCallableSymbol<*>>(boundSymbol, type, useSiteSession, scopeSession)
