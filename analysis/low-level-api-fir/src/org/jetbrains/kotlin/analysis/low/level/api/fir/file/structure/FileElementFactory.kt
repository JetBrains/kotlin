/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirClassSpecificMembersResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.resolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.isImplicitConstructor
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

object FileElementFactory {
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
            firDeclaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            if (firDeclaration is FirPrimaryConstructor) {
                firDeclaration.valueParameters.forEach { parameter ->
                    parameter.correspondingProperty?.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                }
            }

            DeclarationStructureElement(firFile, firDeclaration, moduleComponents)
        }
    }

    fun lazyResolveClassGeneratedMembers(firClass: FirRegularClass) {
        val classMembersToResolve = buildList {
            for (member in firClass.declarations) {
                when {
                    member is FirSimpleFunction && member.source?.kind == KtFakeSourceElementKind.DataClassGeneratedMembers -> {
                        add(member)
                    }

                    member.source?.kind == KtFakeSourceElementKind.EnumGeneratedDeclaration -> {
                        add(member)
                    }

                    member.isImplicitConstructor -> {
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

        if (classMembersToResolve.isEmpty()) return
        val firClassDesignation = firClass.collectDesignation()
        val designationWithMembers = LLFirClassSpecificMembersResolveTarget(
            firClassDesignation,
            classMembersToResolve,
        )

        designationWithMembers.resolve(FirResolvePhase.BODY_RESOLVE)
    }
}
