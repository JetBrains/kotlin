/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.containingDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.isAutonomousDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirProvider
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId
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
                            val firScript = firFile.declarations.singleOrNull() as? FirScript
                            if (declaration is KtScript) {
                                return@findSourceNonLocalFirDeclarationByProvider firScript?.takeIf { it.psi == declaration }
                            }

                            firScript?.declarations
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
        "No fir element was found for ${this::class.simpleName}",
        psi = this,
        fir = firFile,
        additionalInfos = { withEntry("isPhysical", isPhysical.toString()) }
    )
}

@KtAnalysisApiInternals
fun collectUseSiteContainers(element: PsiElement, resolveSession: LLFirResolveSession): List<FirDeclaration>? {
    val containingDeclaration = element.getNonLocalContainingOrThisDeclaration { it.isAutonomousDeclaration } ?: return null
    val containingFile = containingDeclaration.containingKtFile
    val firFile = resolveSession.getOrBuildFirFile(containingFile)
    return FirElementFinder.findPathToDeclarationWithTarget(firFile, containingDeclaration)
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
        is KtAnonymousInitializer,
        is KtTypeAlias,
        is KtDestructuringDeclaration,
        is KtDestructuringDeclarationEntry,
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

fun FirAnonymousInitializer.containingClassIdOrNull(): ClassId? =
    (containingDeclarationSymbol as? FirClassSymbol<*>)?.classId

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

@LLFirInternals
val FirFile.codeFragment: FirCodeFragment
    get() {
        return declarations.singleOrNull() as? FirCodeFragment
            ?: errorWithFirSpecificEntries("Code fragment not found in a FirFile", fir = this)
    }

val FirDeclaration.isGeneratedDeclaration
    get() = realPsi == null

internal inline fun FirScript.forEachDeclaration(action: (FirDeclaration) -> Unit) {
    for (statement in declarations) {
        action(statement)
    }
}

internal inline fun FirRegularClass.forEachDeclaration(action: (FirDeclaration) -> Unit) {
    declarations.forEach(action)
}

internal inline fun FirFile.forEachDeclaration(action: (FirDeclaration) -> Unit) {
    declarations.forEach(action)
}

internal val FirDeclaration.isDeclarationContainer: Boolean get() = this is FirRegularClass || this is FirScript || this is FirFile

internal inline fun FirDeclaration.forEachDeclaration(action: (FirDeclaration) -> Unit) {
    when (this) {
        is FirRegularClass -> forEachDeclaration(action)
        is FirScript -> forEachDeclaration(action)
        is FirFile -> forEachDeclaration(action)
        else -> errorWithFirSpecificEntries("Unsupported declarations container", fir = this)
    }
}

/**
 * Some "local" declarations are not local from the lazy resolution perspective.
 */
internal val FirCallableSymbol<*>.isLocalForLazyResolutionPurposes: Boolean
    get() = when {
        // We should treat result$$ property as non-local explicitly as its CallableId is local
        // TODO: can be dropped after KT-65523
        fir.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty -> false

        // Destructuring declaration container should be treated as a non-local as it is a top-level script declaration
        fir.origin == FirDeclarationOrigin.Synthetic.ScriptTopLevelDestructuringDeclarationContainer -> false

        // We should treat destructuring declaration entries as non-local explicitly as its CallableId is local
        // TODO: can be dropped after KT-65727
        (fir as? FirProperty)?.destructuringDeclarationContainerVariable != null -> false

        else -> callableId.isLocal || fir.status.visibility == Visibilities.Local
    }

val PsiElement.parentsWithSelfCodeFragmentAware: Sequence<PsiElement>
    get() = generateSequence(this) { element ->
        when (element) {
            is KtCodeFragment -> element.context
            is PsiFile -> null
            else -> element.parent
        }
    }

val PsiElement.parentsCodeFragmentAware: Sequence<PsiElement>
    get() = parentsWithSelfCodeFragmentAware.drop(1)

internal fun <T : PsiElement> T.unwrapCopy(containingFile: PsiFile = this.containingFile): T? {
    val originalFile = containingFile.originalFile.takeIf { it !== containingFile }
        ?: (containingFile as? KtFile)?.analysisContext?.containingFile
        ?: return null

    return try {
        PsiTreeUtil.findSameElementInCopy(this, originalFile)
    } catch (_: IllegalStateException) {
        // File copy has a different file structure
        null
    }
}