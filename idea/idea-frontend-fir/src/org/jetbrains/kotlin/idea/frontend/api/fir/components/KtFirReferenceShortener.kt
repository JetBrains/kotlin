/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenCommand
import org.jetbrains.kotlin.idea.frontend.api.components.KtReferenceShortener
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.addImportToFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

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

        return ShortenCommandImpl(file, typesToImport, typesToShorten.map { it.createSmartPointer() })
    }

    private fun findFirstClassifierInScopesByName(positionScopes: List<FirScope>, targetClassName: Name): ClassId? {
        for (scope in positionScopes) {
            val classifierSymbol = scope.findFirstClassifierByName(targetClassName) ?: continue
            val classifierLookupTag = classifierSymbol.toLookupTag() as? ConeClassLikeLookupTag ?: continue

            return classifierLookupTag.classId
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

    private fun findScopesAtPosition(targetTypeReference: KtElement): List<FirScope>? {
        val towerDataContext = firResolveState.getTowerDataContextForElement(targetTypeReference) ?: return null
        val availableScopes = towerDataContext.towerDataElements.mapNotNull { it.scope }

        return availableScopes.asReversed()
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

            val wholeClassifierId = resolvedTypeRef.type.classId ?: return
            val wholeTypeElement = wholeTypeReference.typeElement as? KtUserType ?: return

            if (wholeTypeElement.qualifier == null) return

            collectTypeIfNeedsToBeShortened(wholeClassifierId, wholeTypeElement)
        }

        private fun collectTypeIfNeedsToBeShortened(wholeClassifierId: ClassId, wholeTypeElement: KtUserType) {
            val allClassIds = generateSequence(wholeClassifierId) { it.outerClassId }
            val allTypeElements = generateSequence(wholeTypeElement) { it.qualifier }

            val positionScopes = findScopesAtPosition(wholeTypeElement) ?: return

            for ((classId, typeElement) in allClassIds.zip(allTypeElements)) {
                val firstFoundClass = findFirstClassifierInScopesByName(positionScopes, classId.shortClassName)

                if (firstFoundClass == classId) {
                    addTypeToShorten(typeElement)
                    return
                }
            }

            // none class matched
            val (mostTopLevelClassId, mostTopLevelTypeElement) = allClassIds.zip(allTypeElements).last()
            val firstFoundClass = findFirstClassifierInScopesByName(positionScopes, mostTopLevelClassId.shortClassName)

            check(firstFoundClass != mostTopLevelClassId) { "This should not be true" }

            if (firstFoundClass == null) {
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
