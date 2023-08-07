/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.codeFragment
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirMultiDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier

internal class RawFirNonLocalDeclarationBuilder private constructor(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    private val originalDeclaration: FirDeclaration,
    private val declarationToBuild: KtElement,
    private val functionsToRebind: Set<FirFunction>? = null,
    private val replacementApplier: RawFirReplacement.Applier? = null,
) : PsiRawFirBuilder(session, baseScopeProvider, bodyBuildingMode = BodyBuildingMode.NORMAL) {
    companion object {
        fun buildNewFile(
            session: FirSession,
            scopeProvider: FirScopeProvider,
            file: KtFile,
        ): FirFile {
            val builder = PsiRawFirBuilder(session, scopeProvider, bodyBuildingMode = BodyBuildingMode.NORMAL)
            return builder.buildFirFile(file)
        }

        fun buildWithReplacement(
            session: FirSession,
            scopeProvider: FirScopeProvider,
            designation: FirDesignation,
            rootNonLocalDeclaration: KtElement,
            replacement: RawFirReplacement?,
        ): FirDeclaration {
            val replacementApplier = replacement?.Applier()
            val result = build(session, scopeProvider, designation, rootNonLocalDeclaration, replacementApplier = replacementApplier)
            replacementApplier?.ensureApplied()
            return result
        }

        fun buildWithFunctionSymbolRebind(
            session: FirSession,
            scopeProvider: FirScopeProvider,
            designation: FirDesignation,
            rootNonLocalDeclaration: KtAnnotated,
        ): FirDeclaration {
            val functionsToRebind = when (val originalDeclaration = designation.target) {
                is FirFunction -> setOf(originalDeclaration)
                is FirProperty -> setOfNotNull(originalDeclaration.getter, originalDeclaration.setter)
                else -> null
            }

            return build(session, scopeProvider, designation, rootNonLocalDeclaration, functionsToRebind)
        }

        fun build(
            session: FirSession,
            scopeProvider: FirScopeProvider,
            designation: FirDesignation,
            rootNonLocalDeclaration: KtElement,
            functionsToRebind: Set<FirFunction>? = null,
            replacementApplier: RawFirReplacement.Applier? = null
        ): FirDeclaration {
            check(rootNonLocalDeclaration is KtDeclaration || rootNonLocalDeclaration is KtCodeFragment)

            val builder = RawFirNonLocalDeclarationBuilder(
                session = session,
                baseScopeProvider = scopeProvider,
                originalDeclaration = designation.target as FirDeclaration,
                declarationToBuild = rootNonLocalDeclaration,
                functionsToRebind = functionsToRebind,
                replacementApplier = replacementApplier
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
        currentFirTypeParameters: List<FirTypeParameterRef>,
    ) {
        if (originalDeclaration is FirTypeParameterRefsOwner && declarationSource?.psi == originalDeclaration.psi) {
            super.addCapturedTypeParameters(status, declarationSource, originalDeclaration.typeParameters)
        } else {
            super.addCapturedTypeParameters(status, declarationSource, currentFirTypeParameters)
        }
    }

    private inner class VisitorWithReplacement(private val containingClass: FirRegularClass?) : Visitor() {
        fun convertDestructuringDeclaration(element: KtDestructuringDeclaration): FirVariable {
            val replacementDeclaration = replacementApplier?.tryReplace(element) ?: element
            requireIsInstance<KtDestructuringDeclaration>(replacementDeclaration)
            return buildErrorTopLevelDestructuringDeclaration(replacementDeclaration.toFirSourceElement())
        }

        override fun convertElement(element: KtElement, original: FirElement?): FirElement? =
            super.convertElement(replacementApplier?.tryReplace(element) ?: element, original)

        override fun convertProperty(
            property: KtProperty,
            ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<*>?,
            ownerRegularClassTypeParametersCount: Int?,
        ): FirProperty {
            val replacementProperty = replacementApplier?.tryReplace(property) ?: property
            check(replacementProperty is KtProperty)
            return super.convertProperty(
                property = replacementProperty,
                ownerRegularOrAnonymousObjectSymbol = ownerRegularOrAnonymousObjectSymbol,
                ownerRegularClassTypeParametersCount = ownerRegularClassTypeParametersCount
            )
        }

        override fun convertPropertyAccessor(
            accessor: KtPropertyAccessor?,
            property: KtProperty,
            propertyTypeRef: FirTypeRef,
            propertySymbol: FirPropertySymbol,
            isGetter: Boolean,
            accessorAnnotationsFromProperty: List<FirAnnotation>,
            parameterAnnotationsFromProperty: List<FirAnnotation>,
        ): FirPropertyAccessor? {
            val replacementAccessor = if (accessor != null) {
                val replacementAccessor = replacementApplier?.tryReplace(accessor) ?: accessor
                check(replacementAccessor is KtPropertyAccessor)
                replacementAccessor
            } else {
                accessor
            }

            return super.convertPropertyAccessor(
                replacementAccessor,
                property,
                propertyTypeRef,
                propertySymbol,
                isGetter,
                accessorAnnotationsFromProperty,
                parameterAnnotationsFromProperty
            )
        }

        override fun convertValueParameter(
            valueParameter: KtParameter,
            functionSymbol: FirFunctionSymbol<*>,
            defaultTypeRef: FirTypeRef?,
            valueParameterDeclaration: ValueParameterDeclaration,
            additionalAnnotations: List<FirAnnotation>,
        ): FirValueParameter {
            val replacementParameter = replacementApplier?.tryReplace(valueParameter) ?: valueParameter
            check(replacementParameter is KtParameter)
            return super.convertValueParameter(
                valueParameter = replacementParameter,
                functionSymbol = functionSymbol,
                defaultTypeRef = defaultTypeRef,
                valueParameterDeclaration = valueParameterDeclaration,
                additionalAnnotations = additionalAnnotations
            )
        }

        private fun extractContructorConversionParams(
            classOrObject: KtClassOrObject,
            constructor: KtConstructor<*>?,
        ): ConstructorConversionParams {
            val typeParameters = mutableListOf<FirTypeParameterRef>()
            context.appendOuterTypeParameters(ignoreLastLevel = false, typeParameters)
            val containingClass = this.containingClass ?: errorWithAttachment("Constructor outside of class") {
                withPsiEntry("constructor", constructor, baseSession.llFirModuleData.ktModule)
            }
            val selfType = classOrObject.toDelegatedSelfType(typeParameters, containingClass.symbol)
            val allSuperTypeCallEntries = classOrObject.superTypeListEntries.filterIsInstance<KtSuperTypeCallEntry>()
            val superTypeCallEntry = allSuperTypeCallEntries.lastOrNull()
            return ConstructorConversionParams(superTypeCallEntry, selfType, typeParameters, allSuperTypeCallEntries)
        }

        override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: FirElement?): FirElement {
            val classOrObject = constructor.getContainingClassOrObject()
            val params = extractContructorConversionParams(classOrObject, constructor)
            val delegatedTypeRef = (originalDeclaration as FirConstructor).delegatedConstructor?.constructedTypeRef
                ?: errorWithAttachment("Secondary constructor without delegated call") {
                    withPsiEntry("constructor", constructor, baseSession.llFirModuleData.ktModule)
                }
            return constructor.toFirConstructor(
                delegatedTypeRef,
                params.selfType,
                classOrObject,
                params.typeParameters,
            )
        }

        fun processPrimaryConstructor(classOrObject: KtClassOrObject, constructor: KtPrimaryConstructor?): FirElement {
            val params = extractContructorConversionParams(classOrObject, constructor)
            val firConstructor = originalDeclaration as FirConstructor
            val allSuperTypeCallEntries = if (params.allSuperTypeCallEntries.size <= 1) {
                params.allSuperTypeCallEntries.map { it to firConstructor.delegatedConstructor!!.constructedTypeRef }
            } else {
                params.allSuperTypeCallEntries.zip((firConstructor.delegatedConstructor as FirMultiDelegatedConstructorCall).delegatedConstructorCalls.map { it.constructedTypeRef })
            }
            val newConstructor = constructor.toFirConstructor(
                params.superTypeCallEntry,
                firConstructor.delegatedConstructor?.constructedTypeRef,
                params.selfType,
                classOrObject,
                params.typeParameters,
                allSuperTypeCallEntries,
                firConstructor.delegatedConstructor == null,
                copyConstructedTypeRefWithImplicitSource = false,
            )
            val delegatedConstructor = firConstructor.delegatedConstructor
            if (delegatedConstructor is FirMultiDelegatedConstructorCall) {
                for ((oldExcessiveDelegate, newExcessiveDelegate) in delegatedConstructor.delegatedConstructorCalls
                    .zip((newConstructor.delegatedConstructor as FirMultiDelegatedConstructorCall).delegatedConstructorCalls)) {
                    val calleeReferenceForExessiveDelegate = oldExcessiveDelegate.calleeReference
                    if (calleeReferenceForExessiveDelegate is FirSuperReference) {
                        (newExcessiveDelegate.calleeReference as? FirSuperReference)
                            ?.replaceSuperTypeRef(calleeReferenceForExessiveDelegate.superTypeRef)
                    }
                }
            } else {
                val calleeReference = delegatedConstructor?.calleeReference
                if (calleeReference is FirSuperReference) {
                    (newConstructor.delegatedConstructor?.calleeReference as? FirSuperReference)?.replaceSuperTypeRef(calleeReference.superTypeRef)
                }
            }
            return newConstructor
        }

        override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, data: FirElement?): FirElement =
            processPrimaryConstructor(constructor.getContainingClassOrObject(), constructor)

        override fun visitEnumEntry(enumEntry: KtEnumEntry, data: FirElement?): FirElement {
            val owner = containingClass ?: errorWithAttachment("Enum entry outside of class") {
                withPsiEntry("enumEntry", enumEntry, baseSession.llFirModuleData.ktModule)
            }
            val classOrObject = owner.psi as KtClassOrObject
            val primaryConstructor = classOrObject.primaryConstructor
            val ownerClassHasDefaultConstructor =
                primaryConstructor?.valueParameters?.isEmpty() ?: classOrObject.secondaryConstructors.let { constructors ->
                    constructors.isEmpty() || constructors.any { it.valueParameters.isEmpty() }
                }
            val typeParameters = mutableListOf<FirTypeParameterRef>()
            context.appendOuterTypeParameters(ignoreLastLevel = false, typeParameters)
            val selfType = classOrObject.toDelegatedSelfType(typeParameters, owner.symbol)
            return enumEntry.toFirEnumEntry(selfType, ownerClassHasDefaultConstructor)
        }

        fun processField(classOrObject: KtClassOrObject, originalDeclaration: FirField): FirField? {
            var index = 0
            classOrObject.superTypeListEntries.forEach { superTypeListEntry ->
                if (superTypeListEntry is KtDelegatedSuperTypeEntry) {
                    val expectedName = NameUtils.delegateFieldName(index)
                    if (originalDeclaration.name == expectedName) {
                        return buildFieldForSupertypeDelegate(
                            superTypeListEntry, superTypeListEntry.typeReference.toFirOrErrorType(), index
                        )
                    }
                    index++
                }
            }
            return null
        }
    }

    private fun moveNext(iterator: Iterator<FirDeclaration>, containingClass: FirRegularClass?): FirDeclaration {
        if (!iterator.hasNext()) {
            val visitor = VisitorWithReplacement(containingClass)
            return when (declarationToBuild) {
                is KtProperty -> {
                    val ownerSymbol = containingClass?.symbol
                    val ownerTypeArgumentsCount = containingClass?.typeParameters?.size
                    visitor.convertProperty(declarationToBuild, ownerSymbol, ownerTypeArgumentsCount)
                }
                is KtConstructor<*> -> {
                    if (containingClass == null) {
                        // Constructor outside of class, syntax error, we should not do anything
                        originalDeclaration
                    } else {
                        visitor.convertElement(declarationToBuild, originalDeclaration)
                    }
                }
                is KtClassOrObject -> {
                    when {
                        originalDeclaration is FirConstructor -> visitor.processPrimaryConstructor(declarationToBuild, null)
                        originalDeclaration is FirField -> visitor.processField(declarationToBuild, originalDeclaration)
                        else -> visitor.convertElement(declarationToBuild, originalDeclaration)
                    }
                }
                is KtDestructuringDeclaration -> visitor.convertDestructuringDeclaration(declarationToBuild)
                is KtCodeFragment -> {
                    val firFile = visitor.convertElement(declarationToBuild, originalDeclaration) as FirFile
                    firFile.codeFragment
                }
                else -> visitor.convertElement(declarationToBuild, originalDeclaration)
            } as FirDeclaration
        }

        val parent = iterator.next()
        if (parent !is FirRegularClass) return moveNext(iterator, containingClass = null)

        val classOrObject = parent.psi
        if (classOrObject !is KtClassOrObject) {
            errorWithFirSpecificEntries("Expected KtClassOrObject is not found", fir = parent, psi = classOrObject)
        }

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

    private data class ConstructorConversionParams(
        val superTypeCallEntry: KtSuperTypeCallEntry?,
        val selfType: FirTypeRef,
        val typeParameters: List<FirTypeParameterRef>,
        val allSuperTypeCallEntries: List<KtSuperTypeCallEntry>,
    )
}

