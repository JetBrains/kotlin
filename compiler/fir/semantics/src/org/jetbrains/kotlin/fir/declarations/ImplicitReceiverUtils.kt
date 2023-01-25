/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.labelName
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.wrapNestedClassifierScopeWithSubstitutionForSuperType
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

fun SessionHolder.collectImplicitReceivers(
    type: ConeKotlinType?,
    owner: FirDeclaration
): ImplicitReceivers {
    val implicitCompanionValues = mutableListOf<ImplicitReceiverValue<*>>()
    val contextReceiverValues = mutableListOf<ContextReceiverValue<*>>()
    val implicitReceiverValue = when (owner) {
        is FirClass -> {
            val towerElementsForClass = collectTowerDataElementsForClass(owner, type!!)
            implicitCompanionValues.addAll(towerElementsForClass.implicitCompanionValues)
            contextReceiverValues.addAll(towerElementsForClass.contextReceivers)

            towerElementsForClass.thisReceiver
        }
        is FirFunction -> {
            contextReceiverValues.addAll(owner.createContextReceiverValues(this))
            type?.let { ImplicitExtensionReceiverValue(owner.symbol, type, session, scopeSession) }
        }
        is FirVariable -> {
            contextReceiverValues.addAll(owner.createContextReceiverValues(this))
            type?.let { ImplicitExtensionReceiverValue(owner.symbol, type, session, scopeSession) }
        }
        else -> {
            if (type != null) {
                throw IllegalArgumentException("Incorrect label & receiver owner: ${owner.javaClass}")
            }

            null
        }
    }
    return ImplicitReceivers(implicitReceiverValue, implicitCompanionValues, contextReceiverValues)
}

data class ImplicitReceivers(
    val implicitReceiverValue: ImplicitReceiverValue<*>?,
    val implicitCompanionValues: List<ImplicitReceiverValue<*>>,
    val contextReceivers: List<ContextReceiverValue<*>>,
)

fun SessionHolder.collectTowerDataElementsForClass(owner: FirClass, defaultType: ConeKotlinType): TowerElementsForClass {
    val allImplicitCompanionValues = mutableListOf<ImplicitReceiverValue<*>>()

    val companionObject = (owner as? FirRegularClass)?.companionObjectSymbol?.fir
    val companionReceiver = companionObject?.let { companion ->
        ImplicitDispatchReceiverValue(
            companion.symbol, session, scopeSession
        )
    }
    allImplicitCompanionValues.addIfNotNull(companionReceiver)

    val superClassesStaticsAndCompanionReceivers = mutableListOf<FirTowerDataElement>()
    for (superType in lookupSuperTypes(owner, lookupInterfaces = false, deep = true, useSiteSession = session, substituteTypes = true)) {
        val expandedType = superType.fullyExpandedType(session)
        val superClass = expandedType.lookupTag.toSymbol(session)?.fir as? FirRegularClass ?: continue

        superClass.staticScope(this)
            ?.wrapNestedClassifierScopeWithSubstitutionForSuperType(expandedType, session)
            ?.asTowerDataElementForStaticScope(staticScopeOwnerSymbol = superClass.symbol)
            ?.let(superClassesStaticsAndCompanionReceivers::add)

        (superClass as? FirRegularClass)?.companionObjectSymbol?.let {
            val superCompanionReceiver = ImplicitDispatchReceiverValue(
                it, session, scopeSession
            )

            superClassesStaticsAndCompanionReceivers += superCompanionReceiver.asTowerDataElement()
            allImplicitCompanionValues += superCompanionReceiver
        }
    }

    val thisReceiver = ImplicitDispatchReceiverValue(owner.symbol, defaultType, session, scopeSession)
    val contextReceivers = (owner as? FirRegularClass)?.contextReceivers?.mapIndexed { index, receiver ->
        ContextReceiverValueForClass(
            owner.symbol, receiver.typeRef.coneType, receiver.labelName, session, scopeSession,
            contextReceiverNumber = index,
        )
    }.orEmpty()

    return TowerElementsForClass(
        thisReceiver,
        contextReceivers,
        owner.staticScope(this),
        companionReceiver,
        companionObject?.staticScope(this),
        superClassesStaticsAndCompanionReceivers.asReversed(),
        allImplicitCompanionValues.asReversed()
    )
}

class TowerElementsForClass(
    val thisReceiver: ImplicitReceiverValue<*>,
    val contextReceivers: List<ContextReceiverValueForClass>,
    val staticScope: FirScope?,
    val companionReceiver: ImplicitReceiverValue<*>?,
    val companionStaticScope: FirScope?,
    // Ordered from inner scopes to outer scopes.
    val superClassesStaticsAndCompanionReceivers: List<FirTowerDataElement>,
    // Ordered from inner scopes to outer scopes.
    val implicitCompanionValues: List<ImplicitReceiverValue<*>>
)

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
            implicitReceiverStack
                .addAll(newElements.mapNotNull { it.implicitReceiver })
                .addAllContextReceivers(newElements.flatMap { it.contextReceiverGroup.orEmpty() }),
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

    fun addReceiver(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>, additionalLabName: Name? = null): FirTowerDataContext {
        val element = implicitReceiverValue.asTowerDataElement()
        return FirTowerDataContext(
            towerDataElements.add(element),
            implicitReceiverStack.add(name, implicitReceiverValue, additionalLabName),
            localScopes,
            nonLocalTowerDataElements.add(element)
        )
    }

    fun addContextReceiverGroup(contextReceiverGroup: ContextReceiverGroup): FirTowerDataContext {
        if (contextReceiverGroup.isEmpty()) return this
        val element = contextReceiverGroup.asTowerDataElement()

        return FirTowerDataContext(
            towerDataElements.add(element),
            contextReceiverGroup.fold(implicitReceiverStack, PersistentImplicitReceiverStack::addContextReceiver),
            localScopes,
            nonLocalTowerDataElements.add(element)
        )
    }

    fun addNonLocalScopeIfNotNull(scope: FirScope?): FirTowerDataContext {
        if (scope == null) return this
        return addNonLocalScope(scope)
    }

    fun addNonLocalScope(scope: FirScope): FirTowerDataContext {
        val element = scope.asTowerDataElement(isLocal = false)
        return FirTowerDataContext(
            towerDataElements.add(element),
            implicitReceiverStack,
            localScopes,
            nonLocalTowerDataElements.add(element)
        )
    }

    fun createSnapshot(): FirTowerDataContext {
        return FirTowerDataContext(
            towerDataElements.map(FirTowerDataElement::createSnapshot).toPersistentList(),
            implicitReceiverStack.createSnapshot(),
            localScopes.toPersistentList(),
            nonLocalTowerDataElements.map(FirTowerDataElement::createSnapshot).toPersistentList()
        )
    }
}

class FirTowerDataElement(
    val scope: FirScope?,
    val implicitReceiver: ImplicitReceiverValue<*>?,
    val contextReceiverGroup: ContextReceiverGroup? = null,
    val isLocal: Boolean,
    val staticScopeOwnerSymbol: FirRegularClassSymbol? = null
) {
    fun createSnapshot(): FirTowerDataElement =
        FirTowerDataElement(
            scope,
            implicitReceiver?.createSnapshot(),
            contextReceiverGroup?.map { it.createSnapshot() },
            isLocal,
            staticScopeOwnerSymbol
        )

    /**
     * Returns [scope] if it is not null. Otherwise, returns [implicitReceiver.implicitScope].
     *
     * Note that a scope for a companion object is an implicit scope.
     */
    fun getAvailableScope() = scope ?: implicitReceiver?.implicitScope
}

fun ImplicitReceiverValue<*>.asTowerDataElement(): FirTowerDataElement =
    FirTowerDataElement(scope = null, implicitReceiver = this, isLocal = false)

fun ContextReceiverGroup.asTowerDataElement(): FirTowerDataElement =
    FirTowerDataElement(scope = null, implicitReceiver = null, contextReceiverGroup = this, isLocal = false)

fun FirScope.asTowerDataElement(isLocal: Boolean): FirTowerDataElement =
    FirTowerDataElement(scope = this, implicitReceiver = null, isLocal = isLocal)

fun FirScope.asTowerDataElementForStaticScope(staticScopeOwnerSymbol: FirRegularClassSymbol?): FirTowerDataElement =
    FirTowerDataElement(scope = this, implicitReceiver = null, isLocal = false, staticScopeOwnerSymbol = staticScopeOwnerSymbol)

fun FirClass.staticScope(sessionHolder: SessionHolder): FirContainingNamesAwareScope? =
    staticScope(sessionHolder.session, sessionHolder.scopeSession)

fun FirClass.staticScope(session: FirSession, scopeSession: ScopeSession): FirContainingNamesAwareScope? =
    scopeProvider.getStaticScope(this, session, scopeSession)

typealias ContextReceiverGroup = List<ContextReceiverValue<*>>
typealias FirLocalScopes = PersistentList<FirLocalScope>

fun FirCallableDeclaration.createContextReceiverValues(
    sessionHolder: SessionHolder,
): List<ContextReceiverValueForCallable> =
    contextReceivers.mapIndexed { index, receiver ->
        ContextReceiverValueForCallable(
            symbol, receiver.typeRef.coneType, receiver.labelName, sessionHolder.session, sessionHolder.scopeSession,
            contextReceiverNumber = index,
        )
    }
