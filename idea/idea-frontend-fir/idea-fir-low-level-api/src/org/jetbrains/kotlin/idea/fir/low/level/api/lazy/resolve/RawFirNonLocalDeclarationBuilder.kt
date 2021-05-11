/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.builder.RawFirBuilderMode
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignation
import org.jetbrains.kotlin.psi.*

internal class RawFirNonLocalDeclarationBuilder private constructor(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    private val declarationToBuild: KtDeclaration,
    private val replacementApplier: RawFirReplacement.Applier? = null
) : RawFirBuilder(session, baseScopeProvider, RawFirBuilderMode.NORMAL) {

    companion object {
        fun build(
            session: FirSession,
            baseScopeProvider: FirScopeProvider,
            designation: FirDeclarationUntypedDesignation,
            rootNonLocalDeclaration: KtDeclaration,
            replacement: RawFirReplacement? = null
        ): FirDeclaration {
            val replacementApplier = replacement?.Applier()
            val builder = RawFirNonLocalDeclarationBuilder(session, baseScopeProvider, rootNonLocalDeclaration, replacementApplier)
            builder.context.packageFqName = rootNonLocalDeclaration.containingKtFile.packageFqName
            return builder.moveNext(designation.path.iterator(), containingClass = null).also {
                replacementApplier?.ensureApplied()
            }
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

        override fun convertValueParameter(valueParameter: KtParameter, defaultTypeRef: FirTypeRef?): FirValueParameter {
            val replacementParameter = replacementApplier?.tryReplace(valueParameter) ?: valueParameter
            check(replacementParameter is KtParameter)
            return super.convertValueParameter(
                valueParameter = replacementParameter,
                defaultTypeRef = defaultTypeRef
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

        withChildClassName(classOrObject.nameAsSafeName, false) {
            withCapturedTypeParameters {
                if (!parent.isInner) context.capturedTypeParameters = context.capturedTypeParameters.clear()
                addCapturedTypeParameters(parent.typeParameters.take(classOrObject.typeParameters.size))
                registerSelfType(classOrObject.toDelegatedSelfType(parent))
                return moveNext(iterator, parent)
            }
        }
    }

    private fun PsiElement?.toDelegatedSelfType(firClass: FirRegularClass): FirResolvedTypeRef =
        toDelegatedSelfType(firClass.typeParameters, firClass.symbol)
}

