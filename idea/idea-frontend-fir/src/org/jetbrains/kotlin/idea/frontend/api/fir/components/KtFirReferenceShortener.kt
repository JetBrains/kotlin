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
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
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

        val typesToImport = mutableListOf<FqName>()
        val typesToShorten = mutableListOf<KtUserType>()

        firFile.acceptChildren(TypesCollectingVisitor(typesToImport, typesToShorten))

        return ShortenCommandImpl(
            file,
            typesToImport.distinct(),
            typesToShorten.distinct().map { it.createSmartPointer() }
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
    private fun findScopesAtPosition(targetTypeReference: KtElement, newImports: List<FqName>): List<FirScope>? {
        val towerDataContext = firResolveState.getTowerDataContextForElement(targetTypeReference) ?: return null

        val result = buildList<FirScope> {
            addAll(towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope })
            addIfNotNull(createFakeImportingScope(targetTypeReference.project, newImports))
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
        private val typesToImport: MutableList<FqName>,
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

            val positionScopes = findScopesAtPosition(wholeTypeElement, typesToImport) ?: return

            for ((classId, typeElement) in allClassIds.zip(allTypeElements)) {
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
            typesToImport.add(classFqName)
            typesToShorten.add(mostTopLevelTypeElement)
        }
    }
}

private class ShortenCommandImpl(
    val targetFile: KtFile,
    val importsToAdd: List<FqName>,
    val typesToShorten: List<SmartPsiElementPointer<KtUserType>>
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
    }
}

private tailrec fun KtTypeElement?.unwrapNullable(): KtTypeElement? =
    if (this is KtNullableType) this.innerType.unwrapNullable() else this
