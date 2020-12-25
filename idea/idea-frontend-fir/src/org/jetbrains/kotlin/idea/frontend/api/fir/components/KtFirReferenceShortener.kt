/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtReferenceShortener
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenCommand
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.addImportToFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.ImportPath

internal class KtFirReferenceShortener(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
    override val firResolveState: FirModuleResolveState,
) : KtReferenceShortener(), KtFirAnalysisSessionComponent {
    override fun collectShortenings(file: KtFile, from: Int, to: Int): ShortenCommand {
        resolveFileToBodyResolve(file)
        val firFile = file.getOrBuildFirOfType<FirFile>(firResolveState)

        val namesToImport = mutableListOf<FqName>()

        val typesToShorten = mutableListOf<KtUserType>()
        val callsToShorten = mutableListOf<KtDotQualifiedExpression>()

        firFile.acceptChildren(TypesCollectingVisitor(namesToImport, typesToShorten))
        firFile.acceptChildren(CallsCollectingVisitor(namesToImport, callsToShorten))

        return ShortenCommandImpl(
            file,
            namesToImport.distinct(),
            typesToShorten.distinct().map { it.createSmartPointer() },
            callsToShorten.distinct().map { it.createSmartPointer() }
        )
    }

    private data class AvailableClassifier(val classId: ClassId, val isFromStarOrPackageImport: Boolean)

    private fun findFirstClassifierInScopesByName(positionScopes: List<FirScope>, targetClassName: Name): AvailableClassifier? {
        for (scope in positionScopes) {
            val classifierSymbol = scope.findFirstClassifierByName(targetClassName) ?: continue
            val classifierLookupTag = classifierSymbol.toLookupTag() as? ConeClassLikeLookupTag ?: continue

            return AvailableClassifier(
                classifierLookupTag.classId,
                isFromStarOrPackageImport = scope is FirAbstractStarImportingScope || scope is FirPackageMemberScope
            )
        }

        return null
    }

    private fun findSingleFunctionInScopesByName(scopes: List<FirScope>, name: Name): FirNamedFunctionSymbol? {
        return scopes.asSequence().mapNotNull { it.getSingleFunctionByName(name) }.singleOrNull()
    }

    private fun findSinglePropertyInScopesByName(scopes: List<FirScope>, name: Name): FirVariableSymbol<*>? {
        return scopes.asSequence().mapNotNull { it.getSinglePropertyByName(name) }.singleOrNull()
    }

    private fun resolveFileToBodyResolve(file: KtFile) {
        for (declaration in file.declarations) {
            declaration.getOrBuildFir(firResolveState) // temporary hack, resolves declaration to BODY_RESOLVE stage
        }
    }

    private fun FirScope.findFirstClassifierByName(name: Name): FirClassifierSymbol<*>? {
        var element: FirClassifierSymbol<*>? = null

        processClassifiersByName(name) {
            if (element == null) {
                element = it
            }
        }

        return element
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun FirScope.getSingleFunctionByName(name: Name): FirNamedFunctionSymbol? =
        buildList { processFunctionsByName(name, this::add) }.singleOrNull()

    @OptIn(ExperimentalStdlibApi::class)
    private fun FirScope.getSinglePropertyByName(name: Name): FirVariableSymbol<*>? =
        buildList { processPropertiesByName(name, this::add) }.singleOrNull()

    @OptIn(ExperimentalStdlibApi::class)
    private fun findScopesAtPosition(position: KtElement, newImports: List<FqName>): List<FirScope>? {
        val towerDataContext = firResolveState.getTowerDataContextForElement(position) ?: return null

        val result = buildList<FirScope> {
            addAll(towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope })
            addIfNotNull(createFakeImportingScope(position.project, newImports))
            addAll(towerDataContext.localScopes)
        }

        return result.asReversed()
    }

    private fun createFakeImportingScope(
        project: Project,
        newImports: List<FqName>
    ): FirScope? {
        if (newImports.isEmpty()) return null

        val psiFactory = KtPsiFactory(project)

        val resolvedNewImports = newImports
            .map { psiFactory.createImportDirective(ImportPath(it, isAllUnder = false)) }
            .mapNotNull { it.getOrBuildFirSafe<FirResolvedImport>(firResolveState) }

        if (resolvedNewImports.isEmpty()) return null

        return FirExplicitSimpleImportingScope(resolvedNewImports, firResolveState.rootModuleSession, ScopeSession())
    }

    private inner class TypesCollectingVisitor(
        private val namesToImport: MutableList<FqName>,
        private val typesToShorten: MutableList<KtUserType>,
    ) : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
            processTypeRef(resolvedTypeRef)

            resolvedTypeRef.acceptChildren(this)
            resolvedTypeRef.delegatedTypeRef?.accept(this)
        }

        private fun processTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
            val wholeTypeReference = resolvedTypeRef.psi as? KtTypeReference ?: return

            val wholeClassifierId = resolvedTypeRef.type.lowerBoundIfFlexible().classId ?: return
            val wholeTypeElement = wholeTypeReference.typeElement.unwrapNullable() as? KtUserType ?: return

            if (wholeTypeElement.qualifier == null) return

            collectTypeIfNeedsToBeShortened(wholeClassifierId, wholeTypeElement)
        }

        private fun collectTypeIfNeedsToBeShortened(wholeClassifierId: ClassId, wholeTypeElement: KtUserType) {
            val allClassIds = generateSequence(wholeClassifierId) { it.outerClassId }
            val allTypeElements = generateSequence(wholeTypeElement) { it.qualifier }

            val positionScopes = findScopesAtPosition(wholeTypeElement, namesToImport) ?: return

            for ((classId, typeElement) in allClassIds.zip(allTypeElements)) {
                // if qualifier is null, then this type have no package and thus cannot be shortened
                if (typeElement.qualifier == null) return

                val firstFoundClass = findFirstClassifierInScopesByName(positionScopes, classId.shortClassName)?.classId

                if (firstFoundClass == classId) {
                    addTypeToShorten(typeElement)
                    return
                }
            }

            // none class matched
            val (mostTopLevelClassId, mostTopLevelTypeElement) = allClassIds.zip(allTypeElements).last()
            val availableClassifier = findFirstClassifierInScopesByName(positionScopes, mostTopLevelClassId.shortClassName)

            check(availableClassifier?.classId != mostTopLevelClassId) { "This should not be true" }

            if (availableClassifier == null || availableClassifier.isFromStarOrPackageImport) {
                addTypeToImportAndShorten(mostTopLevelClassId.asSingleFqName(), mostTopLevelTypeElement)
            }
        }

        private fun addTypeToShorten(typeElement: KtUserType) {
            typesToShorten.add(typeElement)
        }

        private fun addTypeToImportAndShorten(classFqName: FqName, mostTopLevelTypeElement: KtUserType) {
            namesToImport.add(classFqName)
            typesToShorten.add(mostTopLevelTypeElement)
        }
    }

    private inner class CallsCollectingVisitor(
        private val namesToImport: List<FqName>,
        private val callsToShorten: MutableList<KtDotQualifiedExpression>
    ) : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
            super.visitResolvedNamedReference(resolvedNamedReference)

            val referenceExpression = resolvedNamedReference.psi as? KtNameReferenceExpression
            val qualifiedProperty = referenceExpression?.parent as? KtDotQualifiedExpression ?: return

            val propertyId = (resolvedNamedReference.resolvedSymbol as? FirCallableSymbol<*>)?.callableId ?: return

            val scopes = findScopesAtPosition(qualifiedProperty, namesToImport) ?: return
            val singleAvailableProperty = findSinglePropertyInScopesByName(scopes, propertyId.callableName)

            if (singleAvailableProperty?.callableId == propertyId) {
                addElementToShorten(qualifiedProperty)
            }
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall) {
            super.visitFunctionCall(functionCall)

            val callExpression = functionCall.psi as? KtCallExpression ?: return
            val qualifiedCallExpression = callExpression.parent as? KtDotQualifiedExpression ?: return

            val resolvedNamedReference = functionCall.calleeReference as? FirResolvedNamedReference ?: return
            val callableId = (resolvedNamedReference.resolvedSymbol as? FirCallableSymbol<*>)?.callableId ?: return

            val scopes = findScopesAtPosition(callExpression, namesToImport) ?: return
            val singleAvailableCallable = findSingleFunctionInScopesByName(scopes, callableId.callableName)

            if (singleAvailableCallable?.callableId == callableId) {
                addElementToShorten(qualifiedCallExpression)
            }
        }

        private fun addElementToShorten(element: KtDotQualifiedExpression) {
            callsToShorten.add(element)
        }
    }
}

private class ShortenCommandImpl(
    val targetFile: KtFile,
    val importsToAdd: List<FqName>,
    val typesToShorten: List<SmartPsiElementPointer<KtUserType>>,
    val callsToShorten: List<SmartPsiElementPointer<KtDotQualifiedExpression>>,
) : ShortenCommand {

    override fun invokeShortening() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        for (nameToImport in importsToAdd) {
            addImportToFile(targetFile.project, targetFile, nameToImport)
        }

        for (typePointer in typesToShorten) {
            val type = typePointer.element ?: continue
            type.deleteQualifier()
        }

        for (callPointer in callsToShorten) {
            val call = callPointer.element ?: continue
            call.deleteQualifier()
        }
    }
}

private tailrec fun KtTypeElement?.unwrapNullable(): KtTypeElement? =
    if (this is KtNullableType) this.innerType.unwrapNullable() else this

private fun KtDotQualifiedExpression.deleteQualifier(): KtExpression? {
    val selectorExpression = selectorExpression ?: return null
    return this.replace(selectorExpression) as KtExpression
}
