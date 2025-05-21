/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.copyOrigin
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.containingDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.isAutonomousElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirProvider
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.util.OperatorNameConventions

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

@KaImplementationDetail
fun collectUseSiteContainers(element: PsiElement, resolutionFacade: LLResolutionFacade): List<FirDeclaration>? {
    val containingDeclaration = element.getNonLocalContainingOrThisDeclaration { it.isAutonomousElement } ?: return null
    val containingFile = containingDeclaration.containingKtFile
    val firFile = resolutionFacade.getOrBuildFirFile(containingFile)
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
            val ownerDeclaration = ownerDeclaration ?: errorWithFirSpecificEntries(
                "Containing declaration should be not null for ${KtParameter::class.simpleName}",
                psi = this,
            )

            val firDeclaration = ownerDeclaration.findSourceNonLocalFirDeclarationByProvider(
                firDeclarationProvider,
            ) ?: return null

            val parameters = if (isContextParameter) {
                when (firDeclaration) {
                    is FirRegularClass -> firDeclaration.contextParameters
                    is FirCallableDeclaration -> firDeclaration.contextParameters
                    else -> null
                }
            } else {
                (firDeclaration as? FirFunction)?.valueParameters
            }

            parameters?.firstOrNull { it.psi == this }
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
    for (property in parameters) {
        action(property)
    }

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
 * Whether a non-local declaration of the given type supports partial body analysis.
 *
 * The function only checks the declaration type.
 * It does not perform other important checks such as a number of body statements, or even whether the body is present at all.
 */
internal val FirElementWithResolveState.isPartialBodyResolvable: Boolean
    get() = when (this) {
        is FirConstructor -> !isPrimary
        is FirSimpleFunction, is FirAnonymousInitializer -> true
        else -> false
    }

/**
 * Whether a declaration body block supports partial body analysis.
 * For empty blocks and blocks with a single statement, partial analysis is unavailable.
 */
internal val FirBlock.isPartialAnalyzable: Boolean
    get() = statements.size > 1

/**
 * A declaration body (a block with statements).
 */
internal val FirElementWithResolveState.body: FirBlock?
    get() = when (this) {
        is FirFunction -> body
        is FirAnonymousInitializer -> body
        else -> null
    }

/**
 * Some "local" declarations are not local from the lazy resolution perspective.
 */
internal val FirCallableSymbol<*>.isLocalForLazyResolutionPurposes: Boolean
    get() = when (fir.origin) {
        // Destructuring declaration container should be treated as a non-local as it is a top-level script declaration
        FirDeclarationOrigin.Synthetic.ScriptTopLevelDestructuringDeclarationContainer -> false

        // Script parameters should be treated as non-locals as they are visible from FirScript
        FirDeclarationOrigin.ScriptCustomization.Parameter, FirDeclarationOrigin.ScriptCustomization.ParameterFromBaseClass -> false

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
    val originalFile = containingFile.copyOrigin
        ?: (containingFile as? KtFile)?.analysisContext?.containingFile
        ?: return null

    return try {
        PsiTreeUtil.findSameElementInCopy(this, originalFile)
    } catch (_: IllegalStateException) {
        // File copy has a different file structure
        null
    }
}

fun findStringPlusSymbol(session: FirSession): FirNamedFunctionSymbol? {
    val stringClassSymbol = session.builtinTypes.stringType.toRegularClassSymbol(session)
    return stringClassSymbol?.fir?.declarations?.singleOrNull {
        it is FirSimpleFunction && it.name == OperatorNameConventions.PLUS
    }?.symbol as? FirNamedFunctionSymbol
}
