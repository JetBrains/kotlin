/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirClassSpecificMembersResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.resolve
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

internal object FileElementFactory {
    fun createFileStructureElement(
        firDeclaration: FirDeclaration,
        firFile: FirFile,
        moduleComponents: LLFirModuleResolveComponents,
    ): FileStructureElement = when (firDeclaration) {
        is FirRegularClass -> {
            firDeclaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE.previous)

            lazyResolveClassGeneratedMembers(firDeclaration)
            ClassDeclarationStructureElement(firFile, firDeclaration, moduleComponents)
        }

        is FirScript -> {
            firDeclaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE.previous)
            RootScriptStructureElement(firFile, firDeclaration, moduleComponents)
        }

        else -> {
            if (firDeclaration is FirPrimaryConstructor) {
                firDeclaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                firDeclaration.valueParameters.forEach { parameter ->
                    parameter.correspondingProperty?.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                }
            } else {
                /** Reserve the [FirResolvePhase.BODY_RESOLVE] for partial body analysis. */
                firDeclaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE.previous)
            }

            DeclarationStructureElement(firFile, firDeclaration, moduleComponents)
        }
    }

    private fun lazyResolveClassGeneratedMembers(firClass: FirRegularClass) {
        val classMembersToResolve = firClass.declarations.filter(FirDeclaration::isPartOfClassStructureElement)

        if (classMembersToResolve.isEmpty()) return
        val firClassDesignation = firClass.collectDesignation()
        val designationWithMembers = LLFirClassSpecificMembersResolveTarget(
            firClassDesignation,
            classMembersToResolve,
        )

        designationWithMembers.resolve(FirResolvePhase.BODY_RESOLVE)
    }
}
