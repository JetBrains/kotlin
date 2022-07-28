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
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier

internal abstract class RawFirNonLocalBuilder<Output>(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    protected val originalDeclaration: FirTypeParameterRefsOwner?,
) :
    RawFirBuilder(session, baseScopeProvider, psiMode = PsiHandlingMode.IDE, bodyBuildingMode = BodyBuildingMode.NORMAL) {
    override fun addCapturedTypeParameters(
        status: Boolean,
        declarationSource: KtSourceElement?,
        currentFirTypeParameters: List<FirTypeParameterRef>
    ) {
        if (originalDeclaration != null && declarationSource?.psi == originalDeclaration.psi) {
            super.addCapturedTypeParameters(status, declarationSource, originalDeclaration.typeParameters)
        } else {
            super.addCapturedTypeParameters(status, declarationSource, currentFirTypeParameters)
        }
    }

    protected abstract fun process(containingClass: FirRegularClass?): Output
    protected fun moveNext(iterator: Iterator<FirDeclaration>, containingClass: FirRegularClass?): Output {
        if (!iterator.hasNext()) {
            return process(containingClass)
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

internal class RawFirNonLocalDeclarationBuilder private constructor(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    originalDeclaration: FirTypeParameterRefsOwner?,
    private val declarationToBuild: KtDeclaration,
    private val functionsToRebind: Set<FirFunction>? = null,
    private val replacementApplier: RawFirReplacement.Applier? = null
) : RawFirNonLocalBuilder<FirDeclaration>(session, baseScopeProvider, originalDeclaration) {

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
                is FirFunction -> setOf(originalDeclaration)
                is FirProperty -> setOfNotNull(originalDeclaration.getter, originalDeclaration.setter)
                else -> null
            }

            val builder = RawFirNonLocalDeclarationBuilder(
                session = session,
                baseScopeProvider = scopeProvider,
                originalDeclaration = designation.declaration as? FirTypeParameterRefsOwner,
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
        if (originalDeclaration != null && declarationSource?.psi == originalDeclaration.psi) {
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

        private fun extractContructorConversionParams(classOrObject: KtClassOrObject): ConstructorConversionParams {
            val typeParameters = ArrayList<FirTypeParameterRef>()
            context.appendOuterTypeParameters(ignoreLastLevel = false, typeParameters)
            var delegatedSuperTypeRef: FirTypeRef? = null
            var superTypeCallEntry: KtSuperTypeCallEntry? = null
            val selfType = classOrObject.toDelegatedSelfType(typeParameters, FirRegularClassSymbol(context.currentClassId))
            classOrObject.superTypeListEntries.forEachIndexed { _, superTypeListEntry ->
                when (superTypeListEntry) {
                    is KtSuperTypeCallEntry -> {
                        delegatedSuperTypeRef = superTypeListEntry.calleeExpression.typeReference.toFirOrErrorType()
                        superTypeCallEntry = superTypeListEntry
                    }
                }
            }
            if (delegatedSuperTypeRef == null) {
                delegatedSuperTypeRef = if (classOrObject is KtClass && classOrObject.isEnum()) implicitEnumType else implicitAnyType
            }
            return ConstructorConversionParams(superTypeCallEntry, delegatedSuperTypeRef!!, selfType, typeParameters)
        }

        override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, data: Unit): FirElement {
            val classOrObject = constructor.getContainingClassOrObject()
            val params = extractContructorConversionParams(classOrObject)
            return constructor.toFirConstructor(
                params.superTypeCallEntry,
                params.superType,
                params.selfType,
                classOrObject,
                params.typeParameters,
                containingClassIsExpectClass = false
            )
        }

        override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: Unit): FirElement {
            val classOrObject = constructor.getContainingClassOrObject()
            val params = extractContructorConversionParams(classOrObject)
            return constructor.toFirConstructor(params.superType, params.selfType, classOrObject, params.typeParameters)
        }
    }

    override fun process(containingClass: FirRegularClass?): FirDeclaration {
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

    private data class ConstructorConversionParams(
        val superTypeCallEntry: KtSuperTypeCallEntry?,
        val superType: FirTypeRef,
        val selfType: FirTypeRef,
        val typeParameters: List<FirTypeParameterRef>,
    )
}

internal class RawFirNonLocalAnnotationsBuilder private constructor(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    originalDeclaration: FirClassLikeDeclaration,
) : RawFirNonLocalBuilder<List<FirAnnotation>>(
    session,
    baseScopeProvider,
    originalDeclaration
) {
    companion object {
        fun build(
            session: FirSession,
            scopeProvider: FirScopeProvider,
            designation: FirDeclarationDesignation,
            rootNonLocalDeclaration: KtDeclaration,
        ): List<FirAnnotation> {
            val builder = RawFirNonLocalAnnotationsBuilder(
                session = session,
                baseScopeProvider = scopeProvider,
                originalDeclaration = designation.declaration as FirClassLikeDeclaration,
            )
            builder.context.packageFqName = rootNonLocalDeclaration.containingKtFile.packageFqName
            return builder.moveNext(designation.path.iterator(), containingClass = null)
        }
    }

    override fun process(containingClass: FirRegularClass?): List<FirAnnotation> {
        check(originalDeclaration is FirClassLikeDeclaration)
        val classLike = originalDeclaration.psi
        check(classLike is KtClassLikeDeclaration)
        return withChildClassName(classLike.nameAsSafeName, isExpect = classLike.hasExpectModifier() || context.containerIsExpect) {
            AnnotationVisitor().extractAnnotations(classLike)
        }
    }

    private inner class AnnotationVisitor : Visitor() {
        fun extractAnnotations(element: KtAnnotated): List<FirAnnotation> {
            val result = ArrayList<FirAnnotation>()
            element.extractAnnotationsTo(result)
            return result
        }
    }
}
