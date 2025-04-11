/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtFakeSourceElementKind.*
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.isLazyResolvable
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.getContainingClassLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal object LLContainingClassCalculator {
    /**
     * Returns a containing class symbol for the given symbol, computing it solely from the source information
     * and information inside FIR nodes.
     */
    fun getContainingClassSymbol(symbol: FirBasedSymbol<*>): FirClassLikeSymbol<*>? {
        if (!symbol.origin.isLazyResolvable) {
            // Handle only source or source-based declarations for now as below we use the PSI tree
            return null
        }

        if (symbol is FirAnonymousInitializerSymbol) {
            // For anonymous initializers, the containing class symbol is right there, no need in PSI traversal
            return symbol.containingDeclarationSymbol as? FirClassLikeSymbol<*>
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
            return containingClassLookupTag.symbol
        }

        val source = symbol.source as? KtPsiSourceElement ?: return null
        val kind = source.kind

        when (kind) {
            is KtFakeSourceElementKind -> {
                if (symbol is FirConstructorSymbol && kind == ImplicitConstructor) {
                    return computeContainingClass(symbol, source.psi)
                }

                if (symbol is FirPropertyAccessorSymbol) {
                    if (kind == DefaultAccessor) {
                        val containingProperty = source.psi
                        return if (containingProperty is KtProperty || containingProperty is KtParameter) {
                            computeContainingClass(symbol, (containingProperty as KtDeclaration).containingClassOrObject)
                        } else {
                            null
                        }
                    }

                    if (kind == DelegatedPropertyAccessor) {
                        val containingProperty = source.psi as? KtProperty
                        return computeContainingClass(symbol, containingProperty?.containingClassOrObject)
                    }

                    if (kind == PropertyFromParameter) {
                        val containingParameter = source.psi as? KtParameter
                        return computeContainingClass(symbol, containingParameter?.containingClassOrObject)
                    }
                }

                if (symbol is FirPropertySymbol && kind == PropertyFromParameter) {
                    val containingParameter = source.psi as? KtParameter
                    return computeContainingClass(symbol, containingParameter?.containingClassOrObject)
                }

                if (kind == EnumGeneratedDeclaration) {
                    return computeContainingClass(symbol, source.psi)
                }

                if (kind == DataClassGeneratedMembers) {
                    val containingClass = when (val psi = source.psi) {
                        is KtClassOrObject -> psi
                        is KtParameter -> psi.containingClassOrObject // component() functions point to 'KtParameter's
                        is KtPrimaryConstructor -> psi.containingClassOrObject // copy() functions point to either KtClass or KtPrimaryConstructor
                        else -> null
                    }
                    return computeContainingClass(symbol, containingClass)
                }

                if (symbol is FirDanglingModifierSymbol && kind == DanglingModifierList) {
                    val modifierList = source.psi as? KtModifierList
                    val body = modifierList?.parent as? KtClassBody
                    return computeContainingClass(symbol, body?.parent)
                }
            }
            else -> {
                if (symbol is FirClassLikeSymbol<*>) {
                    val selfClass = source.psi as? KtClassOrObject
                    return computeContainingClass(symbol, selfClass?.containingClassOrObject)
                }

                if (symbol is FirCallableSymbol<*>) {
                    val selfCallable = source.psi
                    return when (selfCallable) {
                        is KtCallableDeclaration, is KtEnumEntry -> {
                            computeContainingClass(symbol, selfCallable.containingClassOrObject)
                        }
                        is KtPropertyAccessor -> {
                            val containingProperty = selfCallable.property
                            computeContainingClass(symbol, containingProperty.containingClassOrObject)
                        }
                        else -> null
                    }
                }
            }
        }

        return null
    }

    private fun canHaveContainingClassSymbol(symbol: FirBasedSymbol<*>): Boolean = when (symbol) {
        is FirValueParameterSymbol, is FirAnonymousFunctionSymbol -> false
        is FirPropertySymbol -> !symbol.isLocal
        is FirNamedFunctionSymbol -> !symbol.isLocal
        is FirClassLikeSymbol -> symbol.classId.isNestedClass
        is FirCallableSymbol, is FirDanglingModifierSymbol -> true
        else -> false
    }

    private fun computeContainingClass(symbol: FirBasedSymbol<*>, psi: PsiElement?): FirClassLikeSymbol<*>? {
        if (psi !is KtClassOrObject) {
            return null
        }

        val module = symbol.llFirModuleData.ktModule
        val resolutionFacade = module.getResolutionFacade(module.project)
        return psi.resolveToFirSymbolOfTypeSafe<FirClassLikeSymbol<*>>(resolutionFacade)
    }
}