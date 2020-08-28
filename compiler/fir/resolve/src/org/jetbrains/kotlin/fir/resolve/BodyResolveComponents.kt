/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.FirCallResolver
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitExtensionReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

interface SessionHolder {
    val session: FirSession
    val scopeSession: ScopeSession
}

interface BodyResolveComponents : SessionHolder {
    val returnTypeCalculator: ReturnTypeCalculator
    val implicitReceiverStack: ImplicitReceiverStack
    val containingDeclarations: List<FirDeclaration>
    val fileImportsScope: List<FirScope>
    val towerDataElements: List<FirTowerDataElement>
    val towerDataContext: FirTowerDataContext
    val localScopes: FirLocalScopes
    val towerDataContextForAnonymousFunctions: TowerDataContextForAnonymousFunctions
    val noExpectedType: FirTypeRef
    val symbolProvider: FirSymbolProvider
    val file: FirFile
    val container: FirDeclaration
    val inferenceComponents: InferenceComponents
    val resolutionStageRunner: ResolutionStageRunner
    val samResolver: FirSamResolver
    val callResolver: FirCallResolver
    val callCompleter: FirCallCompleter
    val doubleColonExpressionResolver: FirDoubleColonExpressionResolver
    val syntheticCallGenerator: FirSyntheticCallGenerator
    val dataFlowAnalyzer: FirDataFlowAnalyzer<*>
    val integerLiteralTypeApproximator: IntegerLiteralTypeApproximationTransformer
    val integerOperatorsTypeUpdater: IntegerOperatorsTypeUpdater
    val outerClassManager: FirOuterClassManager

    val <D> AbstractFirBasedSymbol<D>.phasedFir: D where D : FirDeclaration, D : FirSymbolOwner<D>
        get() = phasedFir(FirResolvePhase.DECLARATIONS)
}

typealias FirLocalScopes = PersistentList<FirLocalScope>

class FirTowerDataContext private constructor(
    val towerDataElements: PersistentList<FirTowerDataElement>,
    // These properties are effectively redundant, their content should be consistent with `towerDataElements`,
    // i.e. implicitReceiverStack == towerDataElements.mapNotNull { it.receiver }
    // i.e. localScopes == towerDataElements.mapNotNull { it.scope?.takeIf { it.isLocal } }
    val implicitReceiverStack: PersistentImplicitReceiverStack,
    val localScopes: FirLocalScopes,
    val nonLocalTowerDataElements: PersistentList<FirTowerDataElement>
) {

    constructor() : this(
        persistentListOf(),
        PersistentImplicitReceiverStack(),
        persistentListOf(),
        persistentListOf()
    )

    fun setLastLocalScope(newLastScope: FirLocalScope): FirTowerDataContext {
        val oldLastScope = localScopes.last()
        val indexOfLastLocalScope = towerDataElements.indexOfLast { it.scope === oldLastScope }

        return FirTowerDataContext(
            towerDataElements.set(indexOfLastLocalScope, newLastScope.asTowerDataElement(isLocal = true)),
            implicitReceiverStack,
            localScopes.set(localScopes.lastIndex, newLastScope),
            nonLocalTowerDataElements
        )
    }

    fun addNonLocalTowerDataElements(newElements: List<FirTowerDataElement>): FirTowerDataContext {
        return FirTowerDataContext(
            towerDataElements.addAll(newElements),
            implicitReceiverStack.addAll(newElements.mapNotNull { it.implicitReceiver }),
            localScopes,
            nonLocalTowerDataElements.addAll(newElements)
        )
    }

    fun addLocalScope(localScope: FirLocalScope): FirTowerDataContext {
        return FirTowerDataContext(
            towerDataElements.add(localScope.asTowerDataElement(isLocal = true)),
            implicitReceiverStack,
            localScopes.add(localScope),
            nonLocalTowerDataElements
        )
    }

    fun addReceiver(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>): FirTowerDataContext {
        val element = implicitReceiverValue.asTowerDataElement()
        return FirTowerDataContext(
            towerDataElements.add(element),
            implicitReceiverStack.add(name, implicitReceiverValue),
            localScopes,
            nonLocalTowerDataElements.add(element)
        )
    }

    fun addNonLocalScopeIfNotNull(scope: FirScope?): FirTowerDataContext {
        if (scope == null) return this
        return addNonLocalScope(scope)
    }

    private fun addNonLocalScope(scope: FirScope): FirTowerDataContext {
        val element = scope.asTowerDataElement(isLocal = false)
        return FirTowerDataContext(
            towerDataElements.add(element),
            implicitReceiverStack,
            localScopes,
            nonLocalTowerDataElements.add(element)
        )
    }
}

class FirTowerDataElement(val scope: FirScope?, val implicitReceiver: ImplicitReceiverValue<*>?, val isLocal: Boolean)

fun ImplicitReceiverValue<*>.asTowerDataElement(): FirTowerDataElement =
    FirTowerDataElement(scope = null, this, isLocal = false)

fun FirScope.asTowerDataElement(isLocal: Boolean): FirTowerDataElement =
    FirTowerDataElement(this, implicitReceiver = null, isLocal)

typealias TowerDataContextForAnonymousFunctions = Map<FirAnonymousFunctionSymbol, FirTowerDataContext>

class FirTowerDataContextsForClassParts(
    val forNestedClasses: FirTowerDataContext,
    val forConstructorHeaders: FirTowerDataContext,
    val primaryConstructorParametersScope: FirLocalScope?,
)

// --------------------------------------- Utils ---------------------------------------

data class ImplicitReceivers(
    val implicitReceiverValue: ImplicitReceiverValue<*>?,
    val implicitCompanionValues: List<ImplicitReceiverValue<*>>
)

fun SessionHolder.collectImplicitReceivers(
    type: ConeKotlinType?,
    owner: FirDeclaration
): ImplicitReceivers {
    if (type == null) return ImplicitReceivers(null, emptyList())

    val implicitCompanionValues = mutableListOf<ImplicitReceiverValue<*>>()
    val implicitReceiverValue = when (owner) {
        is FirClass<*> -> {
            val towerElementsForClass = collectTowerDataElementsForClass(owner, type)
            implicitCompanionValues.addAll(towerElementsForClass.implicitCompanionValues)

            towerElementsForClass.thisReceiver
        }
        is FirFunction<*> -> {
            ImplicitExtensionReceiverValue(owner.symbol, type, session, scopeSession)
        }
        is FirVariable<*> -> {
            ImplicitExtensionReceiverValue(owner.symbol, type, session, scopeSession)
        }
        else -> {
            throw IllegalArgumentException("Incorrect label & receiver owner: ${owner.javaClass}")
        }
    }
    return ImplicitReceivers(implicitReceiverValue, implicitCompanionValues.asReversed())
}

fun SessionHolder.collectTowerDataElementsForClass(owner: FirClass<*>, defaultType: ConeKotlinType): TowerElementsForClass {
    val allImplicitCompanionValues = mutableListOf<ImplicitReceiverValue<*>>()

    val companionObject = (owner as? FirRegularClass)?.companionObject
    val companionReceiver = companionObject?.let { companion ->
        ImplicitDispatchReceiverValue(
            companion.symbol, session, scopeSession
        )
    }
    allImplicitCompanionValues.addIfNotNull(companionReceiver)

    val superClassesStaticsAndCompanionReceivers = mutableListOf<FirTowerDataElement>()
    for (superType in lookupSuperTypes(owner, lookupInterfaces = false, deep = true, useSiteSession = session)) {
        val superClass = superType.fullyExpandedType(session).lookupTag.toSymbol(session)?.fir as? FirRegularClass ?: continue

        superClass.staticScope(this)?.asTowerDataElement(isLocal = false)?.let(superClassesStaticsAndCompanionReceivers::add)

        (superClass as? FirRegularClass)?.companionObject?.let { companion ->
            val superCompanionReceiver = ImplicitDispatchReceiverValue(
                companion.symbol, session, scopeSession
            )

            superClassesStaticsAndCompanionReceivers += superCompanionReceiver.asTowerDataElement()
            allImplicitCompanionValues += superCompanionReceiver
        }
    }

    val thisReceiver = ImplicitDispatchReceiverValue(owner.symbol, defaultType, session, scopeSession)

    return TowerElementsForClass(
        thisReceiver,
        owner.staticScope(this),
        companionReceiver,
        companionObject?.staticScope(this),
        superClassesStaticsAndCompanionReceivers,
        allImplicitCompanionValues
    )
}

private fun FirClass<*>.staticScope(sessionHolder: SessionHolder) =
    scopeProvider.getStaticScope(this, sessionHolder.session, sessionHolder.scopeSession)

class TowerElementsForClass(
    val thisReceiver: ImplicitReceiverValue<*>,
    val staticScope: FirScope?,
    val companionReceiver: ImplicitReceiverValue<*>?,
    val companionStaticScope: FirScope?,
    val superClassesStaticsAndCompanionReceivers: List<FirTowerDataElement>,
    val implicitCompanionValues: List<ImplicitReceiverValue<*>>
)
