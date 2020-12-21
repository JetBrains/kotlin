/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

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
import org.jetbrains.kotlin.idea.frontend.api.components.KtReferenceShortener
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenCommand
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

internal class KtFirReferenceShortener(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
    override val firResolveState: FirModuleResolveState,
) : KtReferenceShortener(), KtFirAnalysisSessionComponent {
    override fun collectShortenings(file: KtFile, from: Int, to: Int): ShortenCommand {
        resolveFileToBodyResolve(file)
        val firFile = file.getOrBuildFirOfType<FirFile>(firResolveState)

        val typesToShorten = mutableListOf<KtUserType>()

        firFile.acceptChildren(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                val targetTypeReference = resolvedTypeRef.psi as? KtTypeReference ?: return
                val targetType = targetTypeReference.typeElement as? KtUserType ?: return

                if (targetType.qualifier == null) return

                val targetClassId = resolvedTypeRef.type.classId
                val targetClassName = targetClassId?.shortClassName ?: return

                val positionScopes = findScopesAtPosition(targetTypeReference) ?: return

                val firstFoundClass = positionScopes.asSequence()
                    .mapNotNull { scope -> scope.findFirstClassifierByName(targetClassName) }
                    .mapNotNull { classifierSymbol -> classifierSymbol.toLookupTag() as? ConeClassLikeLookupTag }
                    .map { it.classId }
                    .firstOrNull()

                if (firstFoundClass == null) {
                    // this class should be imported
                }

                if (firstFoundClass == targetClassId) {
                    typesToShorten.add(targetType)
                }
            }
        })

        return ShortenCommand(file, emptyList(), typesToShorten.map { it.createSmartPointer() })
    }

    private fun resolveFileToBodyResolve(file: KtFile) {
        for (declaration in file.declarations) {
            declaration.getOrBuildFir(firResolveState) // temporary hack, resolves declaration to BODY_RESOLVE stage
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun FirScope.findFirstClassifierByName(name: Name): FirClassifierSymbol<*>? =
        buildList { processClassifiersByName(name, this::add) }.firstOrNull()

    private fun findScopesAtPosition(targetTypeReference: KtTypeReference): List<FirScope>? {
        val towerDataContext = firResolveState.getTowerDataContextForElement(targetTypeReference) ?: return null
        val availableScopes = towerDataContext.towerDataElements.mapNotNull { it.scope }

        return availableScopes.asReversed()
    }
}