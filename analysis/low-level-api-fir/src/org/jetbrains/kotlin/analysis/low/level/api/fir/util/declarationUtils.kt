/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.canBePartOfParentDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.containingDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex

internal fun KtDeclaration.findSourceNonLocalFirDeclaration(
    firFileBuilder: LLFirFileBuilder,
    provider: FirProvider,
): FirDeclaration = findSourceNonLocalFirDeclaration(
    firFileBuilder.buildRawFirFileWithCaching(containingKtFile),
    provider,
)

/**
 * 'Non-local' stands for not local classes/functions/etc.
 */
internal fun KtDeclaration.findSourceNonLocalFirDeclaration(firFile: FirFile, provider: FirProvider): FirDeclaration {
    // TODO test what way faster
    if (isPhysical) {
        // do not request providers with non-physical psi in order not to leak them there and
        // to avoid inconsistency between physical psi and its copy during completion
        findSourceNonLocalFirDeclarationByProvider(
            firDeclarationProvider = { declaration ->
                if (declaration is KtClassLikeDeclaration) {
                    declaration.findFir(provider)
                } else {
                    val containingClassOrObject = declaration.containingClassOrObject
                    val declarations = if (containingClassOrObject != null) {
                        val containerClassFir = containingClassOrObject.findFir(provider) as? FirRegularClass
                        containerClassFir?.declarations
                    } else {
                        if (declaration.containingKtFile.isScript()) {
                            // .kts will have a single [FirScript] as a declaration. We need to unwrap statements in it.
                            (firFile.declarations.singleOrNull() as? FirScript)?.statements?.filterIsInstance<FirDeclaration>()
                        } else {
                            firFile.declarations
                        }
                    }

                    // It is possible that we will not be able to find the needed declaration here when the code is invalid
                    // e.g., we have two conflicting declarations with the same name,
                    // and we are searching for the wrong one
                    declarations?.find { it.psi == declaration }
                }
            },
        )?.let { return it }
    }

    findSourceNonLocalFirDeclarationByProvider(
        firDeclarationProvider = { declaration ->
            FirElementFinder.findDeclaration(firFile, declaration)
        },
    )?.let { return it }

    errorWithFirSpecificEntries(
        "No fir element was found for",
        psi = this,
        fir = firFile,
        additionalInfos = { withEntry("isPhysical", isPhysical.toString()) }
    )
}

@KtAnalysisApiInternals
fun PsiElement.collectContainingDeclarationsIfNonLocal(session: LLFirResolveSession): List<FirDeclaration>? {
    val ktFile = containingFile as? KtFile ?: return null
    val ktDeclaration = getNonLocalContainingOrThisDeclaration { !it.canBePartOfParentDeclaration } ?: return null
    val firFile = session.getOrBuildFirFile(ktFile)
    return FirElementFinder.findPathToDeclarationWithTarget(firFile, ktDeclaration)
}

internal fun KtElement.findSourceByTraversingWholeTree(
    firFileBuilder: LLFirFileBuilder,
    containerFirFile: FirFile?,
): FirDeclaration? {
    val firFile = containerFirFile ?: firFileBuilder.buildRawFirFileWithCaching(containingKtFile)
    val originalDeclaration = (this as? KtDeclaration)?.originalDeclaration
    val isDeclaration = this is KtDeclaration
    return FirElementFinder.findElementIn(
        firFile,
        canGoInside = { it is FirRegularClass || it is FirScript || it is FirFunction || it is FirProperty },
        predicate = { firDeclaration ->
            firDeclaration.psi == this || isDeclaration && firDeclaration.psi == originalDeclaration
        }
    )
}

private fun KtDeclaration.findSourceNonLocalFirDeclarationByProvider(
    firDeclarationProvider: (KtDeclaration) -> FirDeclaration?,
): FirDeclaration? {
    val candidate = when (this) {
        is KtClassOrObject,
        is KtProperty,
        is KtNamedFunction,
        is KtConstructor<*>,
        is KtClassInitializer,
        is KtTypeAlias,
        is KtDestructuringDeclaration,
        is KtScript,
        -> firDeclarationProvider(this)

        is KtPropertyAccessor -> {
            val firPropertyDeclaration = property.findSourceNonLocalFirDeclarationByProvider(
                firDeclarationProvider,
            ) as? FirVariable ?: return null

            if (isGetter) {
                firPropertyDeclaration.getter
            } else {
                firPropertyDeclaration.setter
            }
        }

        is KtParameter -> {
            val ownerFunction = ownerFunction ?: errorWithFirSpecificEntries(
                "Containing function should be not null for KtParameter",
                psi = this,
            )

            val firFunctionDeclaration = ownerFunction.findSourceNonLocalFirDeclarationByProvider(
                firDeclarationProvider,
            ) as? FirFunction ?: return null

            firFunctionDeclaration.valueParameters[parameterIndex()]
        }

        is KtTypeParameter -> {
            val declaration = containingDeclaration ?: errorWithFirSpecificEntries(
                "Containing declaration should be not null for KtTypeParameter",
                psi = this,
            )

            val firTypeParameterOwner = declaration.findSourceNonLocalFirDeclarationByProvider(
                firDeclarationProvider,
            ) as? FirTypeParameterRefsOwner ?: return null

            firTypeParameterOwner.typeParameters.firstOrNull { it.psi == this } as FirDeclaration
        }

        else -> errorWithFirSpecificEntries("Invalid container", psi = this)
    }

    //property accessors for properties with delegation have KtFakeSourceElementKind.DelegatedPropertyAccessor kind
    return candidate?.takeIf { it.psi == this }
}

fun FirAnonymousInitializer.containingClass(): FirRegularClass {
    val dispatchReceiverType = this.dispatchReceiverType as? ConeLookupTagBasedType
        ?: error("dispatchReceiverType for FirAnonymousInitializer modifier cannot be null")

    val dispatchReceiverSymbol = dispatchReceiverType.lookupTag.toSymbol(llFirSession)
        ?: error("symbol for FirAnonymousInitializer cannot be null")

    return dispatchReceiverSymbol.fir as FirRegularClass
}

val ORIGINAL_DECLARATION_KEY = com.intellij.openapi.util.Key<KtDeclaration>("ORIGINAL_DECLARATION_KEY")
var KtDeclaration.originalDeclaration by UserDataProperty(ORIGINAL_DECLARATION_KEY)

private val ORIGINAL_KT_FILE_KEY = com.intellij.openapi.util.Key<KtFile>("ORIGINAL_KT_FILE_KEY")
var KtFile.originalKtFile by UserDataProperty(ORIGINAL_KT_FILE_KEY)


private fun KtClassLikeDeclaration.findFir(provider: FirProvider): FirClassLikeDeclaration? {
    return if (provider is LLFirProvider) {
        provider.getFirClassifierByDeclaration(this)
    } else {
        val classId = getClassId() ?: return null
        provider.getFirClassifierByFqName(classId)
    }
}


val FirDeclaration.isGeneratedDeclaration
    get() = realPsi == null
