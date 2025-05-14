/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.wrapNestedClassifierScopeWithSubstitutionForSuperType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStubType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

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
        val superClass = expandedType.lookupTag.toRegularClassSymbol(session)?.fir ?: continue

        superClass.staticScope(this)
            ?.wrapNestedClassifierScopeWithSubstitutionForSuperType(expandedType, session)
            ?.asTowerDataElementForStaticScope(staticScopeOwnerSymbol = superClass.symbol)
            ?.let(superClassesStaticsAndCompanionReceivers::add)

        superClass.companionObjectSymbol?.let {
            val superCompanionReceiver = ImplicitDispatchReceiverValue(
                it, session, scopeSession
            )

            superClassesStaticsAndCompanionReceivers += superCompanionReceiver.asTowerDataElement()
            allImplicitCompanionValues += superCompanionReceiver
        }
    }

    val thisReceiver = ImplicitDispatchReceiverValue(owner.symbol, defaultType, session, scopeSession)
    val contextReceivers = (owner as? FirRegularClass)?.contextParameters?.mapNotNull { receiver ->
        if (receiver.isLegacyContextReceiver()) {
            ContextReceiverValue(
                receiver.symbol, receiver.returnTypeRef.coneType, receiver.name, session, scopeSession,
            )
        } else {
            // We don't support context parameters on classes
            null
        }
    }.orEmpty()

    val phantomStaticThis =
        PhantomStaticThis(owner.symbol, session, scopeSession).takeIf { !owner.classKind.isSingleton }

    return TowerElementsForClass(
        thisReceiver,
        contextReceivers,
        phantomStaticThis,
        owner.staticScope(this),
        companionReceiver,
        companionObject?.staticScope(this),
        superClassesStaticsAndCompanionReceivers.asReversed(),
    )
}

class TowerElementsForClass(
    val thisReceiver: ImplicitReceiverValue<*>,
    val contextReceivers: List<ContextReceiverValue>,
    val staticReceiver: PhantomStaticThis?,
    val staticScope: FirScope?,
    val companionReceiver: ImplicitReceiverValue<*>?,
    val companionStaticScope: FirScope?,
    // Ordered from inner scopes to outer scopes.
    val superClassesStaticsAndCompanionReceivers: List<FirTowerDataElement>,
)

class FirTowerDataContext private constructor(
    val towerDataElements: PersistentList<FirTowerDataElement>,
    // These properties are effectively redundant, their content should be consistent with `towerDataElements`,
    // i.e. implicitReceiverStack == towerDataElements.mapNotNull { it.receiver }
    // i.e. localScopes == towerDataElements.mapNotNull { it.scope?.takeIf { it.isLocal } }
    val implicitValueStorage: ImplicitValueStorage,
    val classesUnderInitialization: PersistentList<FirClassSymbol<*>>,
    val localScopes: FirLocalScopes,
    val nonLocalTowerDataElements: PersistentList<FirTowerDataElement>
) {

    constructor() : this(
        persistentListOf(),
        ImplicitValueStorage(),
        persistentListOf(),
        persistentListOf(),
        persistentListOf()
    )

    fun setLastLocalScope(newLastScope: FirLocalScope): FirTowerDataContext {
        val oldLastScope = localScopes.last()
        val indexOfLastLocalScope = towerDataElements.indexOfLast { it.scope === oldLastScope }

        return FirTowerDataContext(
            towerDataElements.set(indexOfLastLocalScope, newLastScope.asTowerDataElement(isLocal = true)),
            implicitValueStorage,
            classesUnderInitialization,
            localScopes.set(localScopes.lastIndex, newLastScope),
            nonLocalTowerDataElements
        )
    }

    fun addNonLocalTowerDataElements(newElements: List<FirTowerDataElement>): FirTowerDataContext {
        return FirTowerDataContext(
            towerDataElements.addAll(newElements),
            implicitValueStorage
                .addAllImplicitReceivers(newElements.mapNotNull { it.implicitReceiver })
                .addAllContexts(
                    newElements.flatMap { it.contextReceiverGroup.orEmpty() },
                    newElements.flatMap { it.contextParameterGroup.orEmpty() }
                ),
            classesUnderInitialization,
            localScopes,
            nonLocalTowerDataElements.addAll(newElements)
        )
    }

    fun addLocalScope(localScope: FirLocalScope): FirTowerDataContext {
        return FirTowerDataContext(
            towerDataElements.add(localScope.asTowerDataElement(isLocal = true)),
            implicitValueStorage,
            classesUnderInitialization,
            localScopes.add(localScope),
            nonLocalTowerDataElements
        )
    }

    fun addReceiver(name: Name?, implicitReceiverValue: ImplicitReceiver<*>, additionalLabName: Name? = null): FirTowerDataContext {
        val element = implicitReceiverValue.asTowerDataElement()
        return FirTowerDataContext(
            towerDataElements.add(element),
            implicitValueStorage.addImplicitReceiver(name, implicitReceiverValue, additionalLabName),
            classesUnderInitialization,
            localScopes,
            nonLocalTowerDataElements.add(element)
        )
    }

    fun addContextGroups(
        contextReceiverGroup: ContextReceiverGroup,
        contextParameterGroup: ContextParameterGroup,
    ): FirTowerDataContext {
        if (contextReceiverGroup.isEmpty() && contextParameterGroup.isEmpty()) return this
        val element = FirTowerDataElement(
            scope = null,
            implicitReceiver = null,
            contextReceiverGroup = contextReceiverGroup,
            contextParameterGroup = contextParameterGroup,
            isLocal = false
        )

        return FirTowerDataContext(
            towerDataElements.add(element),
            implicitValueStorage.addAllContexts(contextReceiverGroup, contextParameterGroup),
            classesUnderInitialization,
            localScopes,
            nonLocalTowerDataElements.add(element)
        )
    }

    fun addAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer): FirTowerDataContext {
        val correspondingClass = anonymousInitializer.containingDeclarationSymbol as? FirClassSymbol<*> ?: return this
        return FirTowerDataContext(
            towerDataElements,
            implicitValueStorage,
            classesUnderInitialization.add(correspondingClass),
            localScopes,
            nonLocalTowerDataElements
        )
    }

    fun addNonLocalScopeIfNotNull(scope: FirScope?): FirTowerDataContext {
        if (scope == null) return this
        return addNonLocalScope(scope)
    }

    // Optimized version for two parameters
    fun addNonLocalScopesIfNotNull(scope1: FirScope?, scope2: FirScope?): FirTowerDataContext {
        return if (scope1 != null) {
            if (scope2 != null) {
                addNonLocalScopeElements(listOf(scope1.asTowerDataElement(isLocal = false), scope2.asTowerDataElement(isLocal = false)))
            } else {
                addNonLocalScope(scope1)
            }
        } else if (scope2 != null) {
            addNonLocalScope(scope2)
        } else {
            this
        }
    }

    fun addNonLocalScope(scope: FirScope): FirTowerDataContext {
        val element = scope.asTowerDataElement(isLocal = false)
        return FirTowerDataContext(
            towerDataElements.add(element),
            implicitValueStorage,
            classesUnderInitialization,
            localScopes,
            nonLocalTowerDataElements.add(element)
        )
    }

    private fun addNonLocalScopeElements(elements: List<FirTowerDataElement>): FirTowerDataContext {
        return FirTowerDataContext(
            towerDataElements.addAll(elements),
            implicitValueStorage,
            classesUnderInitialization,
            localScopes,
            nonLocalTowerDataElements.addAll(elements)
        )
    }

    fun createSnapshot(keepMutable: Boolean): FirTowerDataContext {
        return FirTowerDataContext(
            towerDataElements.map { it.createSnapshot(keepMutable) }.toPersistentList(),
            implicitValueStorage.createSnapshot(keepMutable),
            classesUnderInitialization,
            localScopes.toPersistentList(),
            nonLocalTowerDataElements.map { it.createSnapshot(keepMutable) }.toPersistentList()
        )
    }

    fun replaceTowerDataElements(
        towerDataElements: PersistentList<FirTowerDataElement>,
        nonLocalTowerDataElements: PersistentList<FirTowerDataElement>,
    ): FirTowerDataContext {
        return FirTowerDataContext(
            towerDataElements,
            implicitValueStorage,
            classesUnderInitialization,
            localScopes,
            nonLocalTowerDataElements
        )
    }
}

/**
 * Each FirTowerDataElement has exactly one non-null value among [scope], [implicitReceiver], and [contextReceiverGroup].
 *
 * If [contextReceiverGroup] is not-null, then [contextParameterGroup] is not-null as well.
 * In that case, one of them will be non-empty.
 *
 */
class FirTowerDataElement(
    val scope: FirScope?,
    val implicitReceiver: ImplicitReceiver<*>?,
    val contextReceiverGroup: ContextReceiverGroup? = null,
    val contextParameterGroup: ContextParameterGroup? = null,
    val isLocal: Boolean,
    val staticScopeOwnerSymbol: FirRegularClassSymbol? = null,
) {
    init {
        require((contextReceiverGroup != null) == (contextParameterGroup != null)) {
            "contextReceiverGroup and contextParameterGroup must either be both null or both not-null"
        }
    }

    val implicitContextGroup: List<ImplicitValue<*>>? = if (contextReceiverGroup != null && contextParameterGroup != null) {
        contextReceiverGroup + contextParameterGroup
    } else {
        null
    }

    fun createSnapshot(keepMutable: Boolean): FirTowerDataElement =
        FirTowerDataElement(
            scope,
            implicitReceiver?.createSnapshot(keepMutable),
            contextReceiverGroup?.map { it.createSnapshot(keepMutable) },
            contextParameterGroup?.map { it.createSnapshot(keepMutable) },
            isLocal,
            staticScopeOwnerSymbol
        )

    /**
     * Returns [scope] if it is not null. Otherwise, returns scopes of implicit receivers (including context receivers).
     *
     * Note that a scope for a companion object is an implicit scope.
     */
    fun getAvailableScopes(
        processTypeScope: FirTypeScope.(ConeKotlinType) -> FirTypeScope = { this },
    ): List<FirScope> = when {
        scope != null -> listOf(scope)
        implicitReceiver != null -> listOf(implicitReceiver.getImplicitScope(processTypeScope))
        contextReceiverGroup != null -> contextReceiverGroup.map { it.getImplicitScope(processTypeScope) }
        else -> error("Tower data element is expected to have either scope or implicit receivers.")
    }

    private fun ImplicitReceiver<*>.getImplicitScope(
        processTypeScope: FirTypeScope.(ConeKotlinType) -> FirTypeScope,
    ): FirScope = when (this) {
        is ImplicitReceiverValue<*> -> {
            // N.B.: implicitScope == null when the type sits in a user-defined 'kotlin' package,
            // but there is no '-Xallow-kotlin-package' compiler argument provided
            val implicitScope = implicitScope ?: return FirTypeScope.Empty

            val type = type.fullyExpandedType(useSiteSession)
            if (type is ConeErrorType || type is ConeStubType) FirTypeScope.Empty
            else implicitScope.processTypeScope(type)
        }
        is PhantomStaticThis -> toScope() ?: FirTypeScope.Empty
    }
}

fun ImplicitReceiver<*>.asTowerDataElement(): FirTowerDataElement =
    FirTowerDataElement(scope = null, implicitReceiver = this, isLocal = false)

fun FirScope.asTowerDataElement(isLocal: Boolean): FirTowerDataElement =
    FirTowerDataElement(scope = this, implicitReceiver = null, isLocal = isLocal)

fun FirScope.asTowerDataElementForStaticScope(staticScopeOwnerSymbol: FirRegularClassSymbol?): FirTowerDataElement =
    FirTowerDataElement(scope = this, implicitReceiver = null, isLocal = false, staticScopeOwnerSymbol = staticScopeOwnerSymbol)

fun FirClassSymbol<*>.staticScope(sessionHolder: SessionHolder): FirContainingNamesAwareScope? =
    fir.staticScope(sessionHolder)

fun FirClassSymbol<*>.staticScope(session: FirSession, scopeSession: ScopeSession): FirContainingNamesAwareScope? =
    fir.staticScope(session, scopeSession)

fun FirClass.staticScope(sessionHolder: SessionHolder): FirContainingNamesAwareScope? =
    staticScope(sessionHolder.session, sessionHolder.scopeSession)

fun FirClass.staticScope(session: FirSession, scopeSession: ScopeSession): FirContainingNamesAwareScope? =
    scopeProvider.getStaticScope(this, session, scopeSession)

typealias ContextReceiverGroup = List<ContextReceiverValue>
typealias ContextParameterGroup = List<ImplicitContextParameterValue>
typealias FirLocalScopes = PersistentList<FirLocalScope>
