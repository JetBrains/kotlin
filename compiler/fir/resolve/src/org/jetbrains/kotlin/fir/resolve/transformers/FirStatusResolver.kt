/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirStatusResolver(
    val session: FirSession,
    val scopeSession: ScopeSession
) {
    companion object {
        private val NOT_INHERITED_MODIFIERS: List<FirDeclarationStatusImpl.Modifier> = listOf(
            FirDeclarationStatusImpl.Modifier.ACTUAL,
            FirDeclarationStatusImpl.Modifier.EXPECT,
            FirDeclarationStatusImpl.Modifier.CONST,
            FirDeclarationStatusImpl.Modifier.LATEINIT,
            FirDeclarationStatusImpl.Modifier.TAILREC,
        )

        private val MODIFIERS_FROM_OVERRIDDEN: List<FirDeclarationStatusImpl.Modifier> =
            FirDeclarationStatusImpl.Modifier.values().toList() - NOT_INHERITED_MODIFIERS
    }

    fun resolveStatus(
        declaration: FirDeclaration,
        containingClass: FirClass?,
        containingProperty: FirProperty?,
        isLocal: Boolean
    ): FirResolvedDeclarationStatus {
        return when (declaration) {
            is FirProperty -> resolveStatus(declaration, containingClass, isLocal)
            is FirSimpleFunction -> resolveStatus(declaration, containingClass, isLocal)
            is FirPropertyAccessor -> resolveStatus(declaration, containingClass, containingProperty, isLocal)
            is FirRegularClass -> resolveStatus(declaration, containingClass, isLocal)
            is FirTypeAlias -> resolveStatus(declaration, containingClass, isLocal)
            is FirConstructor -> resolveStatus(declaration, containingClass, isLocal)
            is FirField -> resolveStatus(declaration, containingClass, isLocal)
            else -> error("Unsupported declaration type: ${declaration.render()}")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun resolveStatus(property: FirProperty, containingClass: FirClass?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return resolveStatus(property, property.status, containingClass, null, isLocal) l@{
            if (containingClass == null) return@l emptyList()
            @Suppress("RemoveExplicitTypeArguments") // Workaround for KT-42175
            buildList<FirProperty> {
                val scope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)
                scope.processPropertiesByName(property.name) {}
                scope.processDirectOverriddenPropertiesWithBaseScope(property.symbol) { symbol, _ ->
                    this += symbol.fir
                    ProcessorAction.NEXT
                }
            }.map {
                it.status as FirResolvedDeclarationStatus
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun resolveStatus(function: FirSimpleFunction, containingClass: FirClass?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return resolveStatus(function, function.status, containingClass, null, isLocal) l@{
            if (containingClass == null) return@l emptyList()
            @Suppress("RemoveExplicitTypeArguments") // Workaround for KT-42175
            buildList<FirCallableDeclaration> {
                val scope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)
                val symbol = function.symbol
                scope.processFunctionsByName(function.name) {}
                scope.processDirectOverriddenFunctionsWithBaseScope(symbol) { overriddenSymbol, _ ->
                    this += overriddenSymbol.fir
                    ProcessorAction.NEXT
                }
            }.mapNotNull {
                it.status as? FirResolvedDeclarationStatus
            }
        }
    }

    fun resolveStatus(
        regularClass: FirRegularClass,
        containingClass: FirClass?,
        isLocal: Boolean
    ): FirResolvedDeclarationStatus {
        return resolveStatus(regularClass, regularClass.status, containingClass, null, isLocal) { emptyList() }
    }

    fun resolveStatus(
        typeAlias: FirTypeAlias,
        containingClass: FirClass?,
        isLocal: Boolean
    ): FirResolvedDeclarationStatus {
        return resolveStatus(typeAlias, typeAlias.status, containingClass, null, isLocal) { emptyList() }
    }

    fun resolveStatus(
        propertyAccessor: FirPropertyAccessor,
        containingClass: FirClass?,
        containingProperty: FirProperty?,
        isLocal: Boolean
    ): FirResolvedDeclarationStatus {
        return resolveStatus(propertyAccessor, propertyAccessor.status, containingClass, containingProperty, isLocal) { emptyList() }
    }

    fun resolveStatus(constructor: FirConstructor, containingClass: FirClass?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return resolveStatus(constructor, constructor.status, containingClass, null, isLocal) { emptyList() }
    }

    fun resolveStatus(field: FirField, containingClass: FirClass?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return resolveStatus(field, field.status, containingClass, null, isLocal) { emptyList() }
    }

    fun resolveStatus(enumEntry: FirEnumEntry, containingClass: FirClass?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return resolveStatus(enumEntry, enumEntry.status, containingClass, null, isLocal) { emptyList() }
    }

    private inline fun resolveStatus(
        declaration: FirDeclaration,
        status: FirDeclarationStatus,
        containingClass: FirClass?,
        containingProperty: FirProperty?,
        isLocal: Boolean,
        overriddenExtractor: () -> List<FirResolvedDeclarationStatus>
    ): FirResolvedDeclarationStatus {
        if (status is FirResolvedDeclarationStatus) return status
        require(status is FirDeclarationStatusImpl)

        @Suppress("UNCHECKED_CAST")
        val overriddenStatuses = overriddenExtractor() as List<FirResolvedDeclarationStatusImpl>
        val visibility = when (status.visibility) {
            Visibilities.Unknown -> when {
                isLocal -> Visibilities.Local
                else -> resolveVisibility(declaration, containingClass, containingProperty, overriddenStatuses)
            }
            else -> status.visibility
        }

        val modality = status.modality?.let {
            if (it == Modality.OPEN && containingClass?.classKind == ClassKind.INTERFACE && !declaration.hasOwnBodyOrAccessorBody()) {
                Modality.ABSTRACT
            } else {
                it
            }
        } ?: resolveModality(declaration, containingClass)
        if (overriddenStatuses.isNotEmpty()) {
            for (modifier in MODIFIERS_FROM_OVERRIDDEN) {
                status[modifier] = status[modifier] || overriddenStatuses.fold(false) { acc, overriddenStatus ->
                    acc || overriddenStatus[modifier]
                }
            }
        }

        val parentEffectiveVisibility = when {
            containingProperty != null -> containingProperty.effectiveVisibility
            containingClass is FirRegularClass -> containingClass.effectiveVisibility
            containingClass is FirAnonymousObject -> EffectiveVisibility.Local
            else -> EffectiveVisibility.Public
        }
        val selfEffectiveVisibility = visibility.toEffectiveVisibility(
            containingClass?.symbol?.toLookupTag(), forClass = declaration is FirClass
        )
        val effectiveVisibility = parentEffectiveVisibility.lowerBound(selfEffectiveVisibility, session.typeContext)
        val annotations = ((containingProperty ?: declaration) as? FirAnnotatedDeclaration)?.annotations ?: emptyList()

        val hasPublishedApiAnnotation = annotations.any {
            it.typeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId == StandardClassIds.PublishedApi
        }

        var selfPublishedEffectiveVisibility = runIf(hasPublishedApiAnnotation) {
            visibility.toEffectiveVisibility(
                containingClass?.symbol?.toLookupTag(), forClass = declaration is FirClass, ownerIsPublishedApi = true
            )
        }
        var parentPublishedEffectiveVisibility = when {
            containingProperty != null -> containingProperty.publishedApiEffectiveVisibility
            containingClass is FirRegularClass -> containingClass.publishedApiEffectiveVisibility
            else -> null
        }
        if (selfPublishedEffectiveVisibility != null || parentPublishedEffectiveVisibility != null) {
            selfPublishedEffectiveVisibility = selfPublishedEffectiveVisibility ?: selfEffectiveVisibility
            parentPublishedEffectiveVisibility = parentPublishedEffectiveVisibility ?: parentEffectiveVisibility
            declaration.publishedApiEffectiveVisibility = parentPublishedEffectiveVisibility.lowerBound(
                selfPublishedEffectiveVisibility,
                session.typeContext
            )
        }

        return status.resolved(visibility, modality, effectiveVisibility)
    }

    private fun resolveVisibility(
        declaration: FirDeclaration,
        containingClass: FirClass?,
        containingProperty: FirProperty?,
        overriddenStatuses: List<FirResolvedDeclarationStatusImpl>
    ): Visibility {
        if (declaration is FirConstructor && containingClass?.hasPrivateConstructor() == true) return Visibilities.Private

        val fallbackVisibility = when {
            declaration is FirPropertyAccessor && containingProperty != null -> containingProperty.visibility
            else -> Visibilities.Public
        }

        return overriddenStatuses.map { it.visibility }
            .maxWithOrNull { v1, v2 -> Visibilities.compare(v1, v2) ?: -1 }
            ?.normalize()
            ?: fallbackVisibility
    }

    private fun FirClass.hasPrivateConstructor(): Boolean {
        val classKind = classKind
        return classKind == ClassKind.ENUM_CLASS || classKind == ClassKind.ENUM_ENTRY || modality == Modality.SEALED || this is FirAnonymousObject
    }

    private fun resolveModality(
        declaration: FirDeclaration,
        containingClass: FirClass?,
    ): Modality {
        return when (declaration) {
            is FirRegularClass -> if (declaration.classKind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL
            is FirCallableDeclaration -> {
                when {
                    containingClass == null -> Modality.FINAL
                    containingClass.classKind == ClassKind.INTERFACE -> {
                        when {
                            declaration.visibility == Visibilities.Private ->
                                Modality.FINAL
                            !declaration.hasOwnBodyOrAccessorBody() ->
                                Modality.ABSTRACT
                            else ->
                                Modality.OPEN
                        }
                    }
                    else -> {
                        if (declaration.isOverride && containingClass.modality != Modality.FINAL) Modality.OPEN else Modality.FINAL
                    }
                }
            }
            else -> Modality.FINAL
        }

    }
}

private val FirClass.modality: Modality?
    get() = when (this) {
        is FirRegularClass -> status.modality
        is FirAnonymousObject -> Modality.FINAL
        else -> error("Unknown kind of class: ${this::class}")
    }

private fun FirDeclaration.hasOwnBodyOrAccessorBody(): Boolean {
    return when (this) {
        is FirSimpleFunction -> this.body != null
        is FirProperty -> this.initializer != null || this.getter?.body != null || this.setter?.body != null
        else -> true
    }
}

private object PublishedApiEffectiveVisibilityKey : FirDeclarationDataKey()
var FirDeclaration.publishedApiEffectiveVisibility: EffectiveVisibility? by FirDeclarationDataRegistry.data(PublishedApiEffectiveVisibilityKey)
