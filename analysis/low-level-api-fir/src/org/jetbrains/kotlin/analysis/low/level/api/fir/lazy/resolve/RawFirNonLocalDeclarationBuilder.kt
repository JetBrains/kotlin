/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.codeFragment
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.builder.buildDestructuringVariable
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.FirMultiDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class RawFirNonLocalDeclarationBuilder private constructor(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    private val originalDeclaration: FirDeclaration,
    private val declarationToBuild: KtElement,
    private val functionsToRebind: Set<FirFunction>,
) : PsiRawFirBuilder(session, baseScopeProvider, bodyBuildingMode = BodyBuildingMode.NORMAL) {
    companion object {
        fun buildWithFunctionSymbolRebind(
            session: FirSession,
            scopeProvider: FirScopeProvider,
            designation: FirDesignation,
            rootNonLocalDeclaration: KtAnnotated,
        ): FirDeclaration {
            val functionsToRebind = when (val originalDeclaration = designation.target) {
                is FirFunction -> setOf(originalDeclaration)
                is FirProperty -> setOfNotNull(originalDeclaration.getter, originalDeclaration.setter)
                else -> emptySet()
            }

            return build(session, scopeProvider, designation, rootNonLocalDeclaration, functionsToRebind)
        }

        private fun build(
            session: FirSession,
            scopeProvider: FirScopeProvider,
            designation: FirDesignation,
            rootNonLocalDeclaration: KtElement,
            functionsToRebind: Set<FirFunction>,
        ): FirDeclaration {
            check(rootNonLocalDeclaration is KtDeclaration || rootNonLocalDeclaration is KtCodeFragment)

            val builder = RawFirNonLocalDeclarationBuilder(
                session = session,
                baseScopeProvider = scopeProvider,
                originalDeclaration = designation.target as FirDeclaration,
                declarationToBuild = rootNonLocalDeclaration,
                functionsToRebind = functionsToRebind,
            )

            builder.context.packageFqName = rootNonLocalDeclaration.containingKtFile.packageFqName
            @OptIn(PrivateForInline::class)
            builder.context.forcedContainerSymbol = designation.target.symbol

            return builder.moveNext(designation.path.iterator(), containingDeclaration = null)
        }
    }

    override fun bindFunctionTarget(target: FirFunctionTarget, function: FirFunction) {
        super.bindFunctionTarget(target, computeRebindTarget(function) ?: function)
    }

    /**
     * @return [FirFunction] if another function should be used instead of [function] for [FirFunctionTarget]
     *
     * @see bindFunctionTarget
     * @see functionsToRebind
     */
    private fun computeRebindTarget(function: FirFunction): FirFunction? {
        if (functionsToRebind.isNullOrEmpty()) return null
        val realPsi = function.realPsi
        if (realPsi != null) {
            return functionsToRebind.firstOrNull { it.realPsi == realPsi }
        }

        val accessor = function as? FirPropertyAccessor ?: return null
        val accessorPsi = accessor.psi ?: return null

        return functionsToRebind.firstOrNull { it is FirPropertyAccessor && it.isGetter == accessor.isGetter && it.psi == accessorPsi }
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
        fun convertDestructuringDeclaration(element: KtDestructuringDeclaration, containingDeclaration: FirDeclaration?): FirVariable {
            return if (containingDeclaration is FirScript) {
                withContainerSymbol(containingDeclaration.symbol) {
                    // Annotations from script destructuring declarations are linked to the script itself
                    buildScriptDestructuringDeclaration(element)
                }
            } else {
                buildErrorTopLevelDestructuringDeclaration(element.toFirSourceElement())
            }
        }

        fun convertDestructuringDeclarationEntry(element: KtDestructuringDeclarationEntry): FirVariable {
            requireIsInstance<FirProperty>(originalDeclaration)

            val container = originalDeclaration.destructuringDeclarationContainerVariable?.fir
            requireWithAttachment(container != null, { "Container is not found"}) {
                withFirEntry("originalDeclaration", originalDeclaration)
                withPsiEntry("element", element)
            }

            return buildDestructuringVariable(
                moduleData = baseModuleData,
                container = container,
                element,
                isVar = false,
                localEntries = false,
                index = element.index(),
                configure = { configureScriptDestructuringDeclarationEntry(it, container) },
            )
        }

        private fun KtDestructuringDeclarationEntry.index(): Int {
            val destructuringDeclaration = parent
            requireIsInstance<KtDestructuringDeclaration>(destructuringDeclaration)
            return destructuringDeclaration.entries.indexOf(this)
        }

        fun convertAnonymousInitializer(element: KtAnonymousInitializer, containingDeclaration: FirDeclaration?): FirAnonymousInitializer {
            return buildAnonymousInitializer(element, containingDeclaration?.symbol)
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
                        return buildFieldForSupertypeDelegate(superTypeListEntry, type = null, index)
                    }

                    index++
                }
            }
            return null
        }
    }

    private fun moveNext(iterator: Iterator<FirDeclaration>, containingDeclaration: FirDeclaration?): FirDeclaration {
        if (!iterator.hasNext()) {
            val containingClass = containingDeclaration as? FirRegularClass
            val visitor = VisitorWithReplacement(containingClass)
            return when (declarationToBuild) {
                is KtProperty -> {
                    val ownerSymbol = containingClass?.symbol
                    visitor.convertProperty(declarationToBuild, ownerSymbol)
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
                is KtDestructuringDeclaration -> visitor.convertDestructuringDeclaration(declarationToBuild, containingDeclaration)
                is KtDestructuringDeclarationEntry -> visitor.convertDestructuringDeclarationEntry(declarationToBuild)
                is KtCodeFragment -> {
                    val firFile = visitor.convertElement(declarationToBuild, originalDeclaration) as FirFile
                    firFile.codeFragment
                }
                is KtAnonymousInitializer -> visitor.convertAnonymousInitializer(declarationToBuild, containingDeclaration)
                else -> visitor.convertElement(declarationToBuild, originalDeclaration)
            } as FirDeclaration
        }

        val parent = iterator.next()
        if (parent !is FirRegularClass) return moveNext(iterator, containingDeclaration = parent)

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

