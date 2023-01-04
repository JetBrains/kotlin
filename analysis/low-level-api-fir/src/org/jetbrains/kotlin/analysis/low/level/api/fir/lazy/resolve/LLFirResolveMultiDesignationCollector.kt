/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.utils.addIfNotNull

internal object LLFirResolveMultiDesignationCollector {
    fun getDesignationsToResolve(target: FirElementWithResolveState): List<LLFirDesignationToResolve> {
        val mainDesignation = getMainDesignationToResolve(target) ?: return emptyList()
        val annotationsContainer = mainDesignation.firFile.annotationsContainer
        if (!annotationsContainer.shouldBeResolved()) return listOf(mainDesignation)
        return buildList {
            add(annotationsContainer.collectDesignationWithFile().asMultiDesignation(resolveMembersInsideTarget = false))
            add(mainDesignation)
        }
    }

    private fun getMainDesignationToResolve(target: FirElementWithResolveState): LLFirDesignationToResolve? {
        if (!target.shouldBeResolved()) return null
        return when (target) {
            is FirPropertyAccessor -> getMainDesignationToResolve(target.propertySymbol.fir)
            is FirBackingField -> getMainDesignationToResolve(target.propertySymbol.fir)
            is FirTypeParameter -> getMainDesignationToResolve(target.containingDeclarationSymbol.fir)
            is FirValueParameter -> getMainDesignationToResolve(target.containingFunctionSymbol.fir)
            is FirFile -> createFileDesignation(target)
            else -> {
                val designation = target.tryCollectDesignationWithFile() ?: return null

                when (target) {
                    is FirPrimaryConstructor -> {
                        val resolveTargets = buildList {
                            add(target)
                            /*
                             Primary constructor parameters generate corresponding properties, those properties do not have psi and may never be resolved,
                             so we resolve them here manually
                             */
                            target.valueParameters.forEach { valueParameter ->
                                addIfNotNull(valueParameter.correspondingProperty)
                            }
                        }
                        LLFirDesignationForResolveWithMultipleTargets(
                            designation.firFile,
                            designation.path,
                            resolveTargets,
                            resolveMembersInsideTarget = false,
                        )
                    }
                    is FirRegularClass -> {
                        val classMembersToResolve = buildList {
                            for (member in target.declarations) {
                                when {
                                    member is FirPrimaryConstructor && member.source?.kind == KtFakeSourceElementKind.ImplicitConstructor -> {
                                        add(member)
                                    }
                                    member is FirField && member.source?.kind == KtFakeSourceElementKind.ClassDelegationField -> {
                                        add(member)
                                    }
                                    member is FirDanglingModifierList -> {
                                        add(member)
                                    }
                                }
                            }
                        }
                        LLFirDesignationForResolveWithMembers(
                            designation.firFile,
                            designation.path,
                            target,
                            classMembersToResolve,
                        )
                    }
                    else -> designation.asMultiDesignation(resolveMembersInsideTarget = false)
                }
            }
        }
    }

    private fun createFileDesignation(firFile: FirFile): LLFirDesignationForResolveWithMultipleTargets? {
        val targets = buildList {
            if (firFile.annotationsContainer.annotations.isNotEmpty()) {
                add(firFile.annotationsContainer)
            }
            firFile.declarations.filterTo(this) { it.shouldBeResolved() }
        }
        if (targets.isEmpty()) return null
        return LLFirDesignationForResolveWithMultipleTargets(
            firFile,
            path = emptyList(),
            targets = targets,
            resolveMembersInsideTarget = true
        )
    }

    private fun FirElementWithResolveState.shouldBeResolved() = when (this) {
        is FirDeclaration -> shouldBeResolved()
        is FirFileAnnotationsContainer -> annotations.isNotEmpty()
        else -> throwUnexpectedFirElementError(this)
    }

    private fun FirDeclaration.shouldBeResolved(): Boolean = when (origin) {
        is FirDeclarationOrigin.Source,
        is FirDeclarationOrigin.ImportedFromObjectOrStatic,
        is FirDeclarationOrigin.Delegated,
        is FirDeclarationOrigin.Synthetic,
        is FirDeclarationOrigin.SubstitutionOverride,
        is FirDeclarationOrigin.SamConstructor,
        is FirDeclarationOrigin.IntersectionOverride -> {
            when (this) {
                is FirFile -> true
                is FirSyntheticProperty -> false
                is FirSyntheticPropertyAccessor -> false
                is FirSimpleFunction,
                is FirProperty,
                is FirPropertyAccessor,
                is FirField,
                is FirTypeAlias,
                is FirConstructor -> true
                else -> true
            }
        }
        else -> {
            check(resolveState.resolvePhase == FirResolvePhase.BODY_RESOLVE) {
                "Expected body resolve phase for origin $origin but found $resolveState"
            }
            false
        }
    }
}