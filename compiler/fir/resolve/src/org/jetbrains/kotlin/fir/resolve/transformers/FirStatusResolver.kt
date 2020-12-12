/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol

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

    fun resolveStatus(declaration: FirDeclaration, containingClass: FirClass<*>?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return when (declaration) {
            is FirProperty -> resolveStatus(declaration, containingClass, isLocal)
            is FirSimpleFunction -> resolveStatus(declaration, containingClass, isLocal)
            is FirPropertyAccessor -> resolveStatus(declaration, containingClass, isLocal)
            is FirRegularClass -> resolveStatus(declaration, containingClass, isLocal)
            is FirTypeAlias -> resolveStatus(declaration, containingClass, isLocal)
            is FirConstructor -> resolveStatus(declaration, containingClass, isLocal)
            is FirField -> resolveStatus(declaration, containingClass, isLocal)
            else -> error("Unsupported declaration type: ${declaration.render()}")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun resolveStatus(property: FirProperty, containingClass: FirClass<*>?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return resolveStatus(property, property.status, containingClass, isLocal) l@{
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
    fun resolveStatus(function: FirSimpleFunction, containingClass: FirClass<*>?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return resolveStatus(function, function.status, containingClass, isLocal) l@{
            if (containingClass == null) return@l emptyList()
            @Suppress("RemoveExplicitTypeArguments") // Workaround for KT-42175
            buildList<FirCallableMemberDeclaration<*>> {
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
        containingClass: FirClass<*>?,
        isLocal: Boolean
    ): FirResolvedDeclarationStatus {
        return resolveStatus(regularClass, regularClass.status, containingClass, isLocal) { emptyList() }
    }

    fun resolveStatus(
        typeAlias: FirTypeAlias,
        containingClass: FirClass<*>?,
        isLocal: Boolean
    ): FirResolvedDeclarationStatus {
        return resolveStatus(typeAlias, typeAlias.status, containingClass, isLocal) { emptyList() }
    }

    fun resolveStatus(
        propertyAccessor: FirPropertyAccessor,
        containingClass: FirClass<*>?,
        isLocal: Boolean
    ): FirResolvedDeclarationStatus {
        return resolveStatus(propertyAccessor, propertyAccessor.status, containingClass, isLocal) { emptyList() }
    }

    fun resolveStatus(constructor: FirConstructor, containingClass: FirClass<*>?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return resolveStatus(constructor, constructor.status, containingClass, isLocal) { emptyList() }
    }

    fun resolveStatus(field: FirField, containingClass: FirClass<*>?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return resolveStatus(field, field.status, containingClass, isLocal) { emptyList() }
    }

    fun resolveStatus(enumEntry: FirEnumEntry, containingClass: FirClass<*>?, isLocal: Boolean): FirResolvedDeclarationStatus {
        return resolveStatus(enumEntry, enumEntry.status, containingClass, isLocal) { emptyList() }
    }

    private inline fun resolveStatus(
        declaration: FirDeclaration,
        status: FirDeclarationStatus,
        containingClass: FirClass<*>?,
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
                declaration is FirConstructor && containingClass is FirAnonymousObject -> Visibilities.Private
                else -> resolveVisibility(declaration, containingClass, overriddenStatuses)
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
        return status.resolved(visibility, modality)
    }

    private fun resolveVisibility(
        declaration: FirDeclaration,
        containingClass: FirClass<*>?,
        overriddenStatuses: List<FirResolvedDeclarationStatusImpl>
    ): Visibility {
        if (declaration is FirConstructor && containingClass != null) {
            val classKind = containingClass.classKind
            if ((classKind == ClassKind.ENUM_CLASS || classKind == ClassKind.ENUM_ENTRY || containingClass.modality == Modality.SEALED)) {
                return Visibilities.Private
            }
        }
        return overriddenStatuses.map { it.visibility }
            .maxWithOrNull { v1, v2 -> Visibilities.compare(v1, v2) ?: -1 }
            ?.normalize()
            ?: Visibilities.Public
    }

    private fun resolveModality(
        declaration: FirDeclaration,
        containingClass: FirClass<*>?,
    ): Modality {
        return when (declaration) {
            is FirRegularClass -> if (declaration.classKind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL
            is FirCallableMemberDeclaration<*> -> {
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

private val <F : FirClass<F>> FirClass<F>.modality: Modality?
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
