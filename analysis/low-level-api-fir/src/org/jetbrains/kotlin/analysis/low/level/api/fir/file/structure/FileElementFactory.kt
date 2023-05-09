/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirClassWithSpecificMembersResolveTarget
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.psi.*

internal object FileElementFactory {
    /**
     * should be consistent with [isReanalyzableContainer]
     */
    fun createFileStructureElement(
        firDeclaration: FirDeclaration,
        ktDeclaration: KtDeclaration,
        firFile: FirFile,
        moduleComponents: LLFirModuleResolveComponents,
    ): FileStructureElement = when {
        ktDeclaration is KtNamedFunction && ktDeclaration.isReanalyzableContainer() -> ReanalyzableFunctionStructureElement(
            firFile,
            ktDeclaration,
            (firDeclaration as FirSimpleFunction).symbol,
            ktDeclaration.modificationStamp,
            moduleComponents,
        )

        ktDeclaration is KtProperty && ktDeclaration.isReanalyzableContainer() -> ReanalyzablePropertyStructureElement(
            firFile,
            ktDeclaration,
            (firDeclaration as FirProperty).symbol,
            ktDeclaration.modificationStamp,
            moduleComponents,
        )

        ktDeclaration is KtClassOrObject && ktDeclaration !is KtEnumEntry -> {
            lazyResolveClassWithGeneratedMembers(firDeclaration as FirRegularClass, moduleComponents)
            NonReanalyzableClassDeclarationStructureElement(
                firFile,
                firDeclaration,
                ktDeclaration,
                moduleComponents,
            )
        }

        else -> {
            NonReanalyzableNonClassDeclarationStructureElement(
                firFile,
                firDeclaration,
                ktDeclaration,
                moduleComponents,
            )
        }
    }

    private fun lazyResolveClassWithGeneratedMembers(firClass: FirRegularClass, moduleComponents: LLFirModuleResolveComponents) {
        val classMembersToResolve = buildList {
            for (member in firClass.declarations) {
                when {
                    member is FirPrimaryConstructor && member.source?.kind == KtFakeSourceElementKind.ImplicitConstructor -> {
                        add(member)
                    }

                    member is FirProperty && member.source?.kind == KtFakeSourceElementKind.PropertyFromParameter -> {
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

        val firClassDesignation = firClass.collectDesignationWithFile()
        val designationWithMembers = LLFirClassWithSpecificMembersResolveTarget(
            firClassDesignation.firFile,
            firClassDesignation.path,
            firClass,
            classMembersToResolve,
        )

        moduleComponents.firModuleLazyDeclarationResolver.lazyResolveTarget(
            designationWithMembers,
            FirResolvePhase.BODY_RESOLVE,
            towerDataContextCollector = null
        )
    }
}

/**
 * should be consistent with [createFileStructureElement]
 */
//TODO make internal
fun isReanalyzableContainer(
    ktDeclaration: KtDeclaration,
): Boolean = when (ktDeclaration) {
    is KtNamedFunction -> ktDeclaration.isReanalyzableContainer()
    is KtProperty -> ktDeclaration.isReanalyzableContainer()
    else -> false
}

private fun KtNamedFunction.isReanalyzableContainer() =
    name != null && hasExplicitTypeOrUnit

private fun KtProperty.isReanalyzableContainer() =
    name != null && typeReference != null

private val KtNamedFunction.hasExplicitTypeOrUnit
    get() = hasBlockBody() || typeReference != null
