/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.statusTransformerExtensions
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

class FirStatusResolver(
    override val session: FirSession,
    override val scopeSession: ScopeSession
) : SessionAndScopeSessionHolder {
    companion object {
        private val NOT_INHERITED_MODIFIERS: List<FirDeclarationStatusImpl.Modifier> = listOf(
            FirDeclarationStatusImpl.Modifier.ACTUAL,
            FirDeclarationStatusImpl.Modifier.EXPECT,
            FirDeclarationStatusImpl.Modifier.CONST,
            FirDeclarationStatusImpl.Modifier.LATEINIT,
            FirDeclarationStatusImpl.Modifier.TAILREC,
            FirDeclarationStatusImpl.Modifier.EXTERNAL,
            FirDeclarationStatusImpl.Modifier.OVERRIDE,
            FirDeclarationStatusImpl.Modifier.SUSPEND,
        )

        private val MODIFIERS_FROM_OVERRIDDEN: List<FirDeclarationStatusImpl.Modifier> =
            FirDeclarationStatusImpl.Modifier.entries - NOT_INHERITED_MODIFIERS
    }

    private val extensionStatusTransformers = session.extensionService.statusTransformerExtensions

    private inline fun FirMemberDeclaration.applyExtensionTransformers(
        operation: FirStatusTransformerExtension.(FirDeclarationStatus) -> FirDeclarationStatus
    ): FirDeclarationStatus {
        if (extensionStatusTransformers.isEmpty()) return status
        val declaration = this
        return extensionStatusTransformers.fold(status) { acc, it ->
            if (it.needTransformStatus(declaration)) {
                it.operation(acc)
            } else {
                acc
            }
        }
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
            is FirBackingField -> resolveStatus(declaration, containingClass, isLocal)
            else -> errorWithAttachment("Unsupported declaration type: ${declaration::class.java}") {
                withFirEntry("declaration", declaration)
            }
        }
    }

    fun getOverriddenProperties(
        property: FirProperty,
        containingClass: FirClass?,
    ): List<FirProperty> {
        if (containingClass == null) {
            return emptyList()
        }

        val scope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)

        return buildList {
            scope.processPropertiesByName(property.name) {}
            scope.processDirectOverriddenPropertiesWithBaseScope(property.symbol) { overriddenSymbol, _ ->
                if (session.visibilityChecker.isVisibleForOverriding(
                        candidateInDerivedClass = property, candidateInBaseClass = overriddenSymbol.fir
                    )
                ) {
                    this += overriddenSymbol.fir
                }
                ProcessorAction.NEXT
            }
        }
    }

    fun resolveStatus(
        property: FirProperty,
        containingClass: FirClass?,
        isLocal: Boolean,
        overriddenStatuses: List<FirResolvedDeclarationStatus>? = null,
    ): FirResolvedDeclarationStatus {
        val statuses = overriddenStatuses
            ?: getOverriddenProperties(property, containingClass).map { it.status as FirResolvedDeclarationStatus }

        val status = property.applyExtensionTransformers { transformStatus(it, property, containingClass?.symbol, isLocal) }
        return resolveStatus(property, status, containingClass, null, isLocal, statuses)
    }

    fun getOverriddenFunctions(
        function: FirSimpleFunction,
        containingClass: FirClass?
    ): List<FirSimpleFunction> {
        if (containingClass == null) {
            return emptyList()
        }

        return buildList {
            val scope = containingClass.unsubstitutedScope(
                session,
                scopeSession,
                withForcedTypeCalculator = false,
                memberRequiredPhase = null,
            )

            val symbol = function.symbol
            scope.processFunctionsByName(function.name) {}
            scope.processDirectOverriddenFunctionsWithBaseScope(symbol) { overriddenSymbol, _ ->
                if (session.visibilityChecker.isVisibleForOverriding(
                        candidateInDerivedClass = function, candidateInBaseClass = overriddenSymbol.fir
                    )
                ) {
                    this += overriddenSymbol.fir
                }
                ProcessorAction.NEXT
            }
        }
    }

    fun resolveStatus(
        function: FirSimpleFunction,
        containingClass: FirClass?,
        isLocal: Boolean,
        overriddenStatuses: List<FirResolvedDeclarationStatus>? = null,
    ): FirResolvedDeclarationStatus {
        val status = function.applyExtensionTransformers {
            transformStatus(it, function, containingClass?.symbol, isLocal)
        }

        val statuses = overriddenStatuses
            ?: getOverriddenFunctions(function, containingClass).map { it.status as FirResolvedDeclarationStatus }

        return resolveStatus(function, status, containingClass, null, isLocal, statuses)
    }

    fun resolveStatus(
        firClass: FirClass,
        containingClass: FirClass?,
        isLocal: Boolean
    ): FirResolvedDeclarationStatus {
        val status = when (firClass) {
            is FirRegularClass -> firClass.applyExtensionTransformers { transformStatus(it, firClass, containingClass?.symbol, isLocal) }
            else -> firClass.status
        }
        return resolveStatus(firClass, status, containingClass, null, isLocal, emptyList())
    }

    fun resolveStatus(
        typeAlias: FirTypeAlias,
        containingClass: FirClass?,
        isLocal: Boolean
    ): FirResolvedDeclarationStatus {
        val status = typeAlias.applyExtensionTransformers {
            transformStatus(it, typeAlias, containingClass?.symbol, isLocal)
        }
        return resolveStatus(typeAlias, status, containingClass, null, isLocal, emptyList())
    }

    fun resolveStatus(
        propertyAccessor: FirPropertyAccessor,
        containingClass: FirClass?,
        containingProperty: FirProperty?,
        isLocal: Boolean,
        overriddenStatuses: List<FirResolvedDeclarationStatus> = emptyList(),
    ): FirResolvedDeclarationStatus {
        val status = propertyAccessor.applyExtensionTransformers {
            transformStatus(it, propertyAccessor, containingClass?.symbol, containingProperty, isLocal)
        }
        return resolveStatus(propertyAccessor, status, containingClass, containingProperty, isLocal, overriddenStatuses)
    }

    fun resolveStatus(constructor: FirConstructor, containingClass: FirClass?, isLocal: Boolean): FirResolvedDeclarationStatus {
        val status = constructor.applyExtensionTransformers {
            transformStatus(it, constructor, containingClass?.symbol, isLocal)
        }
        return resolveStatus(constructor, status, containingClass, null, isLocal, emptyList())
    }

    fun resolveStatus(field: FirField, containingClass: FirClass?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return resolveStatus(field, field.status, containingClass, null, isLocal, emptyList())
    }

    private fun resolveStatus(
        backingField: FirBackingField,
        containingClass: FirClass?,
        isLocal: Boolean
    ): FirResolvedDeclarationStatus {
        val status = backingField.applyExtensionTransformers {
            transformStatus(it, backingField, containingClass?.symbol, isLocal)
        }
        return resolveStatus(backingField, status, containingClass, null, isLocal, emptyList())
    }

    fun resolveStatus(enumEntry: FirEnumEntry, containingClass: FirClass?, isLocal: Boolean): FirResolvedDeclarationStatus {
        val status = enumEntry.applyExtensionTransformers {
            transformStatus(it, enumEntry, containingClass?.symbol, isLocal)
        }
        return resolveStatus(enumEntry, status, containingClass, null, isLocal, emptyList())
    }

    private fun resolveStatus(
        declaration: FirDeclaration,
        status: FirDeclarationStatus,
        containingClass: FirClass?,
        containingProperty: FirProperty?,
        isLocal: Boolean,
        overriddenStatuses: List<FirResolvedDeclarationStatus>
    ): FirResolvedDeclarationStatus {
        if (status is FirResolvedDeclarationStatus) return status
        require(status is FirDeclarationStatusImpl)
        val visibility = when (status.visibility) {
            Visibilities.Unknown -> when {
                isLocal -> Visibilities.Local
                else -> resolveVisibility(declaration, containingClass, containingProperty, overriddenStatuses)
            }
            else -> status.visibility
        }

        val modality = status.modality?.let {
            if (it == Modality.OPEN && containingClass?.classKind == ClassKind.INTERFACE && !(declaration.hasOwnBodyOrAccessorBody() || status.isExpect)) {
                Modality.ABSTRACT
            } else {
                it
            }
        } ?: resolveModality(declaration, containingProperty, containingClass)
        if (overriddenStatuses.isNotEmpty()) {
            for (modifier in MODIFIERS_FROM_OVERRIDDEN) {
                status[modifier] = status[modifier] || overriddenStatuses.fold(false) { acc, overriddenStatus ->
                    acc || (overriddenStatus as FirDeclarationStatusImpl)[modifier]
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

        if (!isLocal) {
            val annotations = (containingProperty ?: declaration).annotations
            val parentPublishedEffectiveVisibility = when {
                containingProperty != null -> containingProperty.publishedApiEffectiveVisibility
                containingClass is FirRegularClass -> containingClass.publishedApiEffectiveVisibility
                else -> null
            }

            computePublishedApiEffectiveVisibility(
                annotations,
                visibility,
                selfEffectiveVisibility,
                containingClass?.symbol,
                parentEffectiveVisibility,
                parentPublishedEffectiveVisibility,
                declaration is FirClass,
                session
            )?.let {
                declaration.nonLazyPublishedApiEffectiveVisibility = it
            }
        }

        if (containingClass is FirRegularClass && containingClass.isExpect) {
            status.isExpect = true
        }

        status.hasMustUseReturnValue = computeMustUseReturnValue(declaration, isLocal, containingClass, containingProperty)

        return status.resolved(visibility, modality, effectiveVisibility)
    }

    private fun computeMustUseReturnValue(
        declaration: FirDeclaration,
        isLocal: Boolean,
        containingClass: FirClass?,
        containingProperty: FirProperty?,
    ): Boolean {
        if (declaration !is FirCallableDeclaration) return false

        return session.mustUseReturnValueStatusComponent.computeMustUseReturnValueForCallable(
            session,
            declaration.symbol,
            isLocal,
            containingClass?.symbol,
            containingProperty?.symbol
        )
    }

    private fun resolveVisibility(
        declaration: FirDeclaration,
        containingClass: FirClass?,
        containingProperty: FirProperty?,
        overriddenStatuses: List<FirResolvedDeclarationStatus>
    ): Visibility {
        if (declaration is FirConstructor && containingClass?.hasPrivateConstructor() == true) return Visibilities.Private

        val fallbackVisibility = when {
            declaration is FirPropertyAccessor && containingProperty != null -> containingProperty.visibility
            else -> Visibilities.Public
        }

        if (containingClass?.status?.isData == true &&
            declaration is FirSimpleFunction &&
            declaration.origin == FirDeclarationOrigin.Synthetic.DataClassMember &&
            DataClassResolver.isCopy(declaration.name)
        ) {
            return when {
                containingClass.hasAnnotation(StandardClassIds.Annotations.ExposedCopyVisibility, session) ->
                    Visibilities.Public
                LanguageFeature.DataClassCopyRespectsConstructorVisibility.isEnabled() ||
                        containingClass.hasAnnotation(StandardClassIds.Annotations.ConsistentCopyVisibility, session) ->
                    containingClass.primaryConstructorIfAny(session)?.fir?.visibility ?: fallbackVisibility
                else -> fallbackVisibility
            }
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
        containingProperty: FirProperty?,
        containingClass: FirClass?,
    ): Modality {
        return when (declaration) {
            is FirRegularClass -> if (declaration.classKind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL
            is FirCallableDeclaration -> {
                val containingPropertyModality = containingProperty?.modality
                when {
                    containingClass == null -> Modality.FINAL
                    declaration is FirPropertyAccessor && containingPropertyModality != null -> containingPropertyModality
                    containingClass.classKind == ClassKind.INTERFACE -> {
                        when {
                            declaration.visibility == Visibilities.Private -> Modality.FINAL
                            !declaration.hasOwnBodyOrAccessorBody() -> Modality.ABSTRACT
                            else -> Modality.OPEN
                        }
                    }
                    declaration.isOverride -> Modality.OPEN
                    else -> Modality.FINAL
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
    }

private fun FirDeclaration.hasOwnBodyOrAccessorBody(): Boolean {
    return when (this) {
        is FirSimpleFunction -> this.body != null
        is FirProperty -> this.initializer != null || this.getter?.body != null || this.setter?.body != null
        else -> true
    }
}
