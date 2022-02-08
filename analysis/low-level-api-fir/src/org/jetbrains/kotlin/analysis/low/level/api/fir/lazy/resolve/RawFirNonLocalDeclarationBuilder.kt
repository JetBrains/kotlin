/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignation
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiHandlingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier

internal class RawFirNonLocalDeclarationBuilder private constructor(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    private val originalDeclaration: FirTypeParameterRefsOwner,
    private val declarationToBuild: KtDeclaration,
    private val functionsToRebind: Set<FirFunction>? = null,
    private val replacementApplier: RawFirReplacement.Applier? = null
) : RawFirBuilder(session, baseScopeProvider, psiMode = PsiHandlingMode.IDE, bodyBuildingMode = BodyBuildingMode.NORMAL) {

    companion object {
        fun buildWithReplacement(
            session: FirSession,
            scopeProvider: FirScopeProvider,
            designation: FirDeclarationDesignation,
            rootNonLocalDeclaration: KtDeclaration,
            replacement: RawFirReplacement?
        ): FirDeclaration {
            val replacementApplier = replacement?.Applier()
            val builder = RawFirNonLocalDeclarationBuilder(
                session = session,
                baseScopeProvider = scopeProvider,
                originalDeclaration = designation.declaration as FirTypeParameterRefsOwner,
                declarationToBuild = rootNonLocalDeclaration,
                replacementApplier = replacementApplier
            )
            builder.context.packageFqName = rootNonLocalDeclaration.containingKtFile.packageFqName
            return builder.moveNext(designation.path.iterator(), containingClass = null).also {
                replacementApplier?.ensureApplied()
            }
        }

        fun buildWithFunctionSymbolRebind(
            session: FirSession,
            scopeProvider: FirScopeProvider,
            designation: FirDeclarationDesignation,
            rootNonLocalDeclaration: KtDeclaration,
        ): FirDeclaration {
            val functionsToRebind = when (val originalDeclaration = designation.declaration) {
                is FirSimpleFunction -> setOf(originalDeclaration)
                is FirProperty -> setOfNotNull(originalDeclaration.getter, originalDeclaration.setter)
                else -> null
            }

            val builder = RawFirNonLocalDeclarationBuilder(
                session = session,
                baseScopeProvider = scopeProvider,
                originalDeclaration = designation.declaration as FirTypeParameterRefsOwner,
                declarationToBuild = rootNonLocalDeclaration,
                functionsToRebind = functionsToRebind,
            )
            builder.context.packageFqName = rootNonLocalDeclaration.containingKtFile.packageFqName
            return builder.moveNext(designation.path.iterator(), containingClass = null)
        }
    }

    override fun bindFunctionTarget(target: FirFunctionTarget, function: FirFunction) {
        val rewrittenTarget = functionsToRebind?.firstOrNull { it.realPsi == function.realPsi } ?: function
        super.bindFunctionTarget(target, rewrittenTarget)
    }

    override fun addCapturedTypeParameters(
        status: Boolean,
        declarationSource: KtSourceElement?,
        currentFirTypeParameters: List<FirTypeParameterRef>
    ) {
        if (declarationSource?.psi == originalDeclaration.psi) {
            super.addCapturedTypeParameters(status, declarationSource, originalDeclaration.typeParameters)
        } else {
            super.addCapturedTypeParameters(status, declarationSource, currentFirTypeParameters)
        }
    }

    private inner class VisitorWithReplacement : Visitor() {
        override fun convertElement(element: KtElement): FirElement? =
            super.convertElement(replacementApplier?.tryReplace(element) ?: element)

        override fun convertProperty(
            property: KtProperty,
            ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<*>?,
            ownerRegularClassTypeParametersCount: Int?
        ): FirProperty {
            val replacementProperty = replacementApplier?.tryReplace(property) ?: property
            check(replacementProperty is KtProperty)
            return super.convertProperty(
                property = replacementProperty,
                ownerRegularOrAnonymousObjectSymbol = ownerRegularOrAnonymousObjectSymbol,
                ownerRegularClassTypeParametersCount = ownerRegularClassTypeParametersCount
            )
        }

        override fun convertValueParameter(
            valueParameter: KtParameter,
            defaultTypeRef: FirTypeRef?,
            valueParameterDeclaration: ValueParameterDeclaration,
            additionalAnnotations: List<FirAnnotation>
        ): FirValueParameter {
            val replacementParameter = replacementApplier?.tryReplace(valueParameter) ?: valueParameter
            check(replacementParameter is KtParameter)
            return super.convertValueParameter(
                valueParameter = replacementParameter,
                defaultTypeRef = defaultTypeRef,
                valueParameterDeclaration = valueParameterDeclaration,
                additionalAnnotations = additionalAnnotations
            )
        }
    }

    private fun moveNext(iterator: Iterator<FirDeclaration>, containingClass: FirRegularClass?): FirDeclaration {
        if (!iterator.hasNext()) {
            val visitor = VisitorWithReplacement()
            return when (declarationToBuild) {
                is KtProperty -> {
                    val ownerSymbol = containingClass?.symbol
                    val ownerTypeArgumentsCount = containingClass?.typeParameters?.size
                    visitor.convertProperty(declarationToBuild, ownerSymbol, ownerTypeArgumentsCount)
                }
                else -> visitor.convertElement(declarationToBuild)
            } as FirDeclaration
        }

        val parent = iterator.next()
        if (parent !is FirRegularClass) return moveNext(iterator, containingClass = null)

        val classOrObject = parent.psi
        check(classOrObject is KtClassOrObject)

        withChildClassName(classOrObject.nameAsSafeName, isExpect = classOrObject.hasExpectModifier() || context.containerIsExpect) {
            withCapturedTypeParameters(
                parent.isInner,
                declarationSource = null,
                parent.typeParameters.subList(0, classOrObject.typeParameters.size)
            ) {
                registerSelfType(classOrObject.toDelegatedSelfType(parent))
                return moveNext(iterator, parent)
            }
        }
    }

    private fun PsiElement?.toDelegatedSelfType(firClass: FirRegularClass): FirResolvedTypeRef =
        toDelegatedSelfType(firClass.typeParameters, firClass.symbol)
}

