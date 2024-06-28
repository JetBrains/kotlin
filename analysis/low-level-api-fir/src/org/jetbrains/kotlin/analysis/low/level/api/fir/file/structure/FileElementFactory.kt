/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirClassWithSpecificMembersResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.resolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.isImplicitConstructor
import org.jetbrains.kotlin.fir.declarations.*

internal object FileElementFactory {
    fun createFileStructureElement(
        firDeclaration: FirDeclaration,
        firFile: FirFile,
        moduleComponents: LLFirModuleResolveComponents,
    ): FileStructureElement = when {
        firDeclaration is FirRegularClass -> {
            lazyResolveClassWithGeneratedMembers(firDeclaration)
            ClassDeclarationStructureElement(firFile, firDeclaration, moduleComponents)
        }

        firDeclaration is FirScript -> RootScriptStructureElement(firFile, firDeclaration, moduleComponents)
        else -> DeclarationStructureElement(firFile, firDeclaration, moduleComponents)
    }

    private fun lazyResolveClassWithGeneratedMembers(firClass: FirRegularClass) {
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

        val firClassDesignation = firClass.collectDesignation()
        val designationWithMembers = LLFirClassWithSpecificMembersResolveTarget(
            firClassDesignation,
            classMembersToResolve,
        )

        designationWithMembers.resolve(FirResolvePhase.BODY_RESOLVE)
    }
}
