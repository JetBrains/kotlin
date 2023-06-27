/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirClassWithSpecificMembersResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

internal object FileElementFactory {
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
                    member is FirSimpleFunction && member.source?.kind == KtFakeSourceElementKind.DataClassGeneratedMembers -> {
                        add(member)
                    }

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
 * @return The declaration in which a change of the passed receiver parameter can be treated as in-block modification
 */
@LLFirInternals
@Suppress("unused") // Used in the IDE plugin
fun PsiElement.getNonLocalReanalyzableContainingDeclaration(): KtDeclaration? {
    return when (val declaration = getNonLocalContainingOrThisDeclaration()) {
        is KtNamedFunction -> declaration.takeIf { function ->
            function.isReanalyzableContainer() && function.bodyExpression?.isAncestor(this) == true
        }

        is KtPropertyAccessor -> declaration.takeIf { accessor ->
            accessor.isReanalyzableContainer() && accessor.bodyExpression?.isAncestor(this) == true
        }

        is KtProperty -> declaration.takeIf { property ->
            property.isReanalyzableContainer() && property.delegateExpressionOrInitializer?.isAncestor(this) == true
        }

        else -> null
    }
}

private fun KtNamedFunction.isReanalyzableContainer(): Boolean = name != null && (hasBlockBody() || typeReference != null)

private fun KtPropertyAccessor.isReanalyzableContainer(): Boolean {
    val property = property
    return property.name != null && (hasBlockBody() || property.typeReference != null)
}

private fun KtProperty.isReanalyzableContainer(): Boolean =
    name != null && typeReference != null && (isTopLevel || !hasDelegateExpressionOrInitializer())

