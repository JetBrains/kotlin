/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtFakeSourceElementKind.*
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.isLazyResolvable
import org.jetbrains.kotlin.fir.getContainingClassLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrScript

@OptIn(KtExperimentalApi::class)
internal object LLContainingClassCalculator {
    /**
     * Returns a containing class symbol for the given symbol, computing it solely from the source information
     * and information inside FIR nodes.
     */
    fun getContainingClassSymbol(symbol: FirBasedSymbol<*>): FirClassSymbol<*>? {
        if (!symbol.origin.isLazyResolvable) {
            // Handle only source or source-based declarations for now as below we use the PSI tree
            return null
        }

        if (symbol is FirAnonymousInitializerSymbol) {
            // For anonymous initializers, the containing class symbol is right there, no need in PSI traversal
            return symbol.containingDeclarationSymbol as? FirClassSymbol<*>
        }

        if (!canHaveContainingClassSymbol(symbol)) {
            return null
        }

        val containingClassLookupTag = when (symbol) {
            is FirCallableSymbol<*> -> symbol.containingClassLookupTag()
            is FirClassLikeSymbol<*> -> symbol.getContainingClassLookupTag()
            is FirDanglingModifierSymbol -> symbol.containingClassLookupTag()
            else -> null
        }

        // For members of local classes lookup tag should be used to avoid a phase
        // contract violation
        if (containingClassLookupTag is ConeClassLikeLookupTagWithFixedSymbol) {
            return containingClassLookupTag.symbol as? FirClassSymbol<*>
        }

        val source = symbol.source as? KtPsiSourceElement ?: return null
        return when (source.kind) {
            ImplicitConstructor,
            is EnumGeneratedDeclaration,
            ReplEvalFunction,
                -> computeContainingClass(symbol, source.psi)

            is DefaultAccessor,
            is DelegatedPropertyAccessor,
            PropertyFromParameter,
                -> computeContainingClass(symbol, (source.psi as? KtDeclaration)?.containingClassOrScript)

            is DataClassGeneratedMembers -> {
                val containingClass = when (val psi = source.psi) {
                    is KtClassOrObject -> psi
                    is KtParameter -> psi.containingClassOrObject // component() functions point to 'KtParameter's
                    is KtPrimaryConstructor -> psi.containingClassOrObject // copy() functions point to either KtClass or KtPrimaryConstructor
                    else -> null
                }

                computeContainingClass(symbol, containingClass)
            }

            DanglingModifierList -> {
                val modifierList = source.psi as? KtModifierList
                val body = modifierList?.parent as? KtClassBody
                computeContainingClass(symbol, body?.parent)
            }

            is KtFakeSourceElementKind -> null

            // TODO(KT-85643): KtRealSourceElementKind should be converted to KtFakeSourceElementKind once the issue is fixed
            is KtRealSourceElementKind if symbol.origin is FirDeclarationOrigin.SubstitutionOverride -> when (val psi = source.psi) {
                // Substituted callables usually have the containing psi as a source element
                // Note: KtCallableDeclaration cannot be used since if present it points to the original callables which has no relation
                // to the containing class
                is KtClassLikeDeclaration -> computeContainingClass(symbol, psi)
                else -> null
            }

            is KtRealSourceElementKind -> when (symbol) {
                is FirCallableSymbol<*>, is FirClassLikeSymbol<*> -> when (val selfCallable = source.psi) {
                    is KtCallableDeclaration, is KtEnumEntry, is KtClassLikeDeclaration, is KtScriptInitializer -> {
                        computeContainingClass(symbol, selfCallable.containingClassOrScript)
                    }

                    is KtPropertyAccessor -> {
                        val containingProperty = selfCallable.property
                        computeContainingClass(symbol, containingProperty.containingClassOrScript)
                    }

                    else -> null
                }

                else -> null
            }
        }
    }

    private fun canHaveContainingClassSymbol(symbol: FirBasedSymbol<*>): Boolean = when (symbol) {
        is FirValueParameterSymbol, is FirAnonymousFunctionSymbol -> false
        is FirRegularPropertySymbol -> true
        is FirNamedFunctionSymbol -> symbol.rawStatus.visibility != Visibilities.Local
        is FirClassLikeSymbol -> symbol.classId.isNestedClass
        is FirCallableSymbol, is FirDanglingModifierSymbol -> true
        else -> false
    }

    private fun computeContainingClass(symbol: FirBasedSymbol<*>, psi: PsiElement?): FirClassSymbol<*>? {
        if (psi !is KtClassOrObject && (psi !is KtScript || !psi.isReplSnippet)) {
            return null
        }

        val module = symbol.llFirModuleData.ktModule
        val resolutionFacade = module.getResolutionFacade(module.project)
        return when (val symbol = psi.resolveToFirSymbol(resolutionFacade)) {
            is FirClassSymbol<*> -> symbol
            is FirReplSnippetSymbol -> symbol.snippetClassSymbol
            else -> null
        }
    }
}
