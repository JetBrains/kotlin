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

internal data class RawFirReplacement<T : KtElement>(val from: T, val to: T)

internal class RawFirNonLocalDeclarationBuilder<T : KtElement> private constructor(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    private val declarationToBuild: KtDeclaration,
    private val replacement: RawFirReplacement<T>? = null
) : RawFirBuilder(session, baseScopeProvider, RawFirBuilderMode.NORMAL) {

    private var replacementApplied = false

    companion object {
        fun elementIsApplicable(element: KtElement) = when (element) {
            is KtFile, is KtClassInitializer, is KtClassOrObject, is KtObjectLiteralExpression, is KtTypeAlias,
            is KtNamedFunction, is KtLambdaExpression, is KtAnonymousInitializer, is KtProperty, is KtTypeReference,
            is KtAnnotationEntry, is KtTypeParameter, is KtTypeProjection, is KtParameter, is KtBlockExpression,
            is KtSimpleNameExpression, is KtConstantExpression, is KtStringTemplateExpression, is KtReturnExpression,
            is KtTryExpression, is KtIfExpression, is KtWhenExpression, is KtDoWhileExpression, is KtWhileExpression,
            is KtForExpression, is KtBreakExpression, is KtContinueExpression, is KtBinaryExpression, is KtBinaryExpressionWithTypeRHS,
            is KtIsExpression, is KtUnaryExpression, is KtCallExpression, is KtArrayAccessExpression, is KtQualifiedExpression,
            is KtThisExpression, is KtSuperExpression, is KtParenthesizedExpression, is KtLabeledExpression, is KtAnnotatedExpression,
            is KtThrowExpression, is KtDestructuringDeclaration, is KtClassLiteralExpression, is KtCallableReferenceExpression,
            is KtCollectionLiteralExpression -> true
            else -> false
        }

        fun <T : KtElement> buildWithReplacement(
            session: FirSession,
            baseScopeProvider: FirScopeProvider,
            designation: FirDeclarationUntypedDesignation,
            declarationToBuild: KtDeclaration,
            replacement: RawFirReplacement<T>? = null
        ): FirDeclaration {

            if (replacement != null) {
                require(elementIsApplicable(replacement.from)) {
                    "Build with replacement is possible for applicable type but given ${replacement.from::class.simpleName}"
                }
                require(replacement.from::class == replacement.to::class) {
                    "Build with replacement is possible for same type in replacements but given\n${replacement.from::class.simpleName} and ${replacement.to::class.simpleName}"
                }
            }

            val builder = RawFirNonLocalDeclarationBuilder(session, baseScopeProvider, declarationToBuild, replacement)
            builder.context.packageFqName = declarationToBuild.containingKtFile.packageFqName

            val result = builder.moveNext(designation.path.iterator(), containingClass = null)
            check(replacement == null || builder.replacementApplied) {
                "Replacement requested but was not applied for ${replacement!!.from::class.simpleName}"
            }
            return result
        }

        fun build(
            session: FirSession,
            baseScopeProvider: FirScopeProvider,
            designation: FirDeclarationUntypedDesignation,
            rootNonLocalDeclaration: KtDeclaration
        ): FirDeclaration {
            val builder = RawFirNonLocalDeclarationBuilder<KtElement>(session, baseScopeProvider, rootNonLocalDeclaration)
            builder.context.packageFqName = rootNonLocalDeclaration.containingKtFile.packageFqName
            return builder.moveNext(designation.path.iterator(), containingClass = null)
        }
    }

    private fun KtElement.replaced(): KtElement {
        if (replacement == null || replacement.from != this) return this
        replacementApplied = true
        return replacement.to
    }

    private inner class VisitorWithReplacement : Visitor() {
        override fun convertElement(element: KtElement): FirElement? =
            super.convertElement(element.replaced())

        override fun convertProperty(
            property: KtProperty,
            ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<*>?,
            ownerRegularClassTypeParametersCount: Int?
        ): FirProperty {
            val replacementProperty = property.replaced()
            check(replacementProperty is KtProperty)
            return super.convertProperty(
                property = replacementProperty,
                ownerRegularOrAnonymousObjectSymbol = ownerRegularOrAnonymousObjectSymbol,
                ownerRegularClassTypeParametersCount = ownerRegularClassTypeParametersCount
            )
        }

        override fun convertValueParameter(valueParameter: KtParameter, defaultTypeRef: FirTypeRef?): FirValueParameter {
            val replacementParameter = valueParameter.replaced()
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

