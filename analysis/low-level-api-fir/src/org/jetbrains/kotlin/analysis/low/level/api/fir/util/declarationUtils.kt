/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.util.withPsiEntry
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.copyOrigin
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.containingDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.isAutonomousElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirProvider
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isCopiedDelegatedProperty
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

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
                    val declarations = when {
                        containingClassOrObject != null -> {
                            val containerClassFir = containingClassOrObject.findFir(provider) as? FirRegularClass
                            containerClassFir?.declarations
                        }

                        declaration.containingKtFile.isScript() -> {
                            // .kts will have a single [FirScript] as a declaration. We need to unwrap statements in it.
                            val scriptOrReplSnippet = firFile.scriptOrReplSnippet
                            when {
                                declaration is KtScript -> return@findSourceNonLocalFirDeclarationByProvider scriptOrReplSnippet?.takeIf { it.psi == declaration }
                                scriptOrReplSnippet == null -> null
                                scriptOrReplSnippet is FirScript -> scriptOrReplSnippet.declarations
                                scriptOrReplSnippet is FirReplSnippet -> scriptOrReplSnippet.snippetClass.declarations
                                else -> errorWithAttachment("Unsupported case: ${scriptOrReplSnippet::class.simpleName}")
                            }
                        }

                        else -> firFile.declarations
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
    return FirElementFinder.findElementIn(
        firFile,
        canGoInside = {
            when (it) {
                is FirRegularClass,
                is FirScript,
                is FirFunction,
                is FirProperty,
                is FirReplSnippet,
                    -> true

                else -> false
            }
        },
        predicate = { firDeclaration ->
            firDeclaration.psi == this
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

internal fun FirAnonymousInitializer.containingClassIdOrNull(): ClassId? =
    (containingDeclarationSymbol as? FirClassSymbol<*>)?.classId

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

internal val FirDeclaration.isGeneratedDeclaration
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

internal inline fun FirReplSnippet.forEachDeclaration(action: (FirDeclaration) -> Unit) {
    action(snippetClass)
}

internal val FirDeclaration.isDeclarationContainer: Boolean
    get() = this is FirRegularClass || this is FirScript || this is FirFile || this is FirReplSnippet

internal inline fun FirDeclaration.forEachDeclaration(action: (FirDeclaration) -> Unit) {
    when (this) {
        is FirRegularClass -> forEachDeclaration(action)
        is FirScript -> forEachDeclaration(action)
        is FirFile -> forEachDeclaration(action)
        is FirReplSnippet -> forEachDeclaration(action)
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
        is FirNamedFunction, is FirAnonymousInitializer -> true
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
@OptIn(FirImplementationDetail::class)
internal val FirCallableSymbol<*>.isLocalForLazyResolutionPurposes: Boolean
    get() = when (fir.origin) {
        // Destructuring declaration container should be treated as a non-local as it is a top-level script declaration
        FirDeclarationOrigin.Synthetic.ScriptTopLevelDestructuringDeclarationContainer -> false

        // Script parameters should be treated as non-locals as they are visible from FirScript
        FirDeclarationOrigin.ScriptCustomization.Parameter, FirDeclarationOrigin.ScriptCustomization.ParameterFromBaseClass -> false

        else -> isLocal || when (val fir = fir) {
            // This is a hack to avoid lazy resolve for copied properties which are resolved as a part of the eval function
            // TODO(KT-85633): drop once the issue is resolved
            is FirProperty -> fir.isCopiedDelegatedProperty == true
            else -> false
        }
    }

internal val PsiElement.parentsWithSelfCodeFragmentAware: Sequence<PsiElement>
    get() = generateSequence(this) { element ->
        when (element) {
            is KtCodeFragment -> element.context
            is PsiFile -> null
            else -> element.parent
        }
    }

internal val PsiElement.parentsCodeFragmentAware: Sequence<PsiElement>
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

@KaImplementationDetail
fun findStringPlusSymbol(session: FirSession): FirNamedFunctionSymbol? {
    val stringClassSymbol = session.builtinTypes.stringType.toRegularClassSymbol(session)
    return stringClassSymbol?.fir?.declarations?.singleOrNull {
        it is FirNamedFunction && it.name == OperatorNameConventions.PLUS
    }?.symbol as? FirNamedFunctionSymbol
}

internal fun PsiClass.classIdOrError(): ClassId =
    classId
        ?: errorWithAttachment("No classId for non-local class") {
            withPsiEntry(
                "psiClass",
                this@classIdOrError,
                KotlinProjectStructureProvider.getModule(project, this@classIdOrError, useSiteModule = null)
            )
            withEntry("qualifiedName", qualifiedName)
        }

@KaImplementationDetail
val PsiClass.classId: ClassId?
    get() {
        val packageName = (containingFile as? PsiClassOwner)?.packageName ?: return null
        if (qualifiedName == null) return null

        val classesChain = generateSequence(this) { it.containingClass }
        if (classesChain.any { it is PsiAnonymousClass }) return null

        val classNames = classesChain.mapTo(mutableListOf()) { it.name }.asReversed()
        if (classNames.any { it == null }) return null

        return ClassId(FqName(packageName), FqName(classNames.joinToString(separator = ".")), isLocal = false)
    }

@KaImplementationDetail
fun PsiClass.isLocalClass(): Boolean {
    val qualifiedName = this.qualifiedName ?: return true
    val classId = classId ?: return true

    /*
    For a local class:
    qualifiedName: javax.swing.JSlider$1SmartHashtable.LabelUIResource
    classId.asFqNameString(): javax.swing.JSlider.SmartHashtable.LabelUIResource

    For a nested class with:
    qualifiedName: pckg.A$B
    classId.asFqNameString(): pckg.A.B

    For a class with $ in name:
    qualifiedName: pckg.With$InName
    classId.asFqNameString(): pckg.With$InName
     */
    return classId.asFqNameString().replace('$', '.') != qualifiedName.replace('$', '.')
}
