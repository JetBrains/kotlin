/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirDanglingFileSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryOrLibrarySourceResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.nullableJavaSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

/**
 * This class describes where locates [target] element and its essential [path].
 *
 * Usually a resolver uses [path] to resolve [target] in the proper context.
 *
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
 */
class FirDesignation(
    /**
     * The path to [target] element.
     *
     * ### Contracts:
     * * Can contain [FirFile] only in the first position
     * * Can contain [FirScript] only in the first or second position
     *
     * @see file
     * @see fileOrNull
     * @see script
     * @see scriptOrNull
     */
    val path: List<FirDeclaration>,
    val target: FirElementWithResolveState,
) {
    constructor(target: FirElementWithResolveState) : this(emptyList(), target)

    init {
        for ((index, declaration) in path.withIndex()) {
            when (declaration) {
                is FirFile -> requireWithAttachment(
                    index == 0,
                    { "${FirFile::class.simpleName} can be only in the first position of the path, but actual is '$index'" },
                ) {
                    withFirDesignationEntry("designation", this@FirDesignation)
                }

                is FirScript -> requireWithAttachment(
                    index == 0 || index == 1 && path.first() is FirFile,
                    { "${FirScript::class.simpleName} can be only in the first or second position of the path, but actual is '$index'" },
                ) {
                    withFirDesignationEntry("designation", this@FirDesignation)
                }

                is FirRegularClass -> {}
                else -> errorWithAttachment("Unexpected declaration type: ${declaration::class.simpleName}") {
                    withFirDesignationEntry("designation", this@FirDesignation)
                }
            }
        }
    }

    val file: FirFile
        get() = fileOrNull ?: errorWithAttachment("File is not found") {
            withFirDesignationEntry("designation", this@FirDesignation)
        }

    val fileOrNull: FirFile? get() = path.firstOrNull() as? FirFile ?: target as? FirFile

    val script: FirScript
        get() = scriptOrNull ?: errorWithAttachment("Script is not found") {
            withFirDesignationEntry("designation", this@FirDesignation)
        }

    val scriptOrNull: FirScript? get() = path.getOrNull(0) as? FirScript ?: path.getOrNull(1) as? FirScript ?: target as? FirScript

    override fun toString(): String = path.plus(target).joinToString(separator = " -> ") {
        it::class.simpleName ?: it.toString()
    }
}

fun ExceptionAttachmentBuilder.withFirDesignationEntry(name: String, designation: FirDesignation) {
    withEntryGroup(name) {
        for ((index, declaration) in designation.path.withIndex()) {
            withFirEntry("path$index", declaration)
        }

        withFirEntry("target", designation.target)
    }
}

fun FirDesignation.toSequence(includeTarget: Boolean): Sequence<FirElementWithResolveState> = sequence {
    yieldAll(path)
    if (includeTarget) yield(target)
}

private fun tryCollectDesignation(providedFile: FirFile?, target: FirElementWithResolveState): FirDesignation? {
    if (target !is FirDeclaration) {
        unexpectedElementError<FirDeclaration>(target)
    }

    return when (target) {
        is FirSyntheticProperty,
        is FirSyntheticPropertyAccessor,
        is FirReplSnippet,
        is FirAnonymousFunction,
        is FirErrorFunction,
        is FirAnonymousObject,
        is FirPropertyAccessor,
        is FirBackingField,
        is FirTypeParameter,
        is FirValueParameter,
        is FirReceiverParameter,
            -> null

        is FirNamedFunction,
        is FirProperty,
        is FirField,
        is FirConstructor,
        is FirEnumEntry,
            -> {
            // We shouldn't try to build a designation path for such fake declarations as they
            // do not depend on outer classes during resolution
            if (target.canHaveDeferredReturnTypeCalculation) return FirDesignation(target)

            if (target.symbol.isLocalForLazyResolutionPurposes) {
                return null
            }

            val containingClassId = target.containingClassLookupTag()?.classId
            collectDesignationPathWithContainingClass(providedFile, target, containingClassId)
        }

        is FirClassLikeDeclaration -> {
            if (target.isLocal) {
                return null
            }

            val containingClassId = target.symbol.classId.outerClassId
            collectDesignationPathWithContainingClass(providedFile, target, containingClassId)
        }

        is FirDanglingModifierList -> {
            val containingClassId = target.containingClass()?.classId
            collectDesignationPathWithContainingClass(providedFile, target, containingClassId)
        }

        is FirAnonymousInitializer -> {
            val containingClassId = target.containingClassIdOrNull()
            collectDesignationPathWithContainingClass(providedFile, target, containingClassId)
        }

        is FirFile -> FirDesignation(target)
        is FirScript, is FirCodeFragment -> {
            collectDesignationPathWithContainingClass(providedFile, target, containingClassId = null)
        }
    }
}

private fun collectDesignationPathWithContainingClass(
    providedFile: FirFile?,
    target: FirDeclaration,
    containingClassId: ClassId?,
): FirDesignation? {
    @OptIn(ClassIdBasedLocality::class)
    if (containingClassId?.isLocal == true) {
        return null
    }

    val file = providedFile ?: target.getContainingFile()
    if (file != null && (containingClassId == null || file.packageFqName == containingClassId.packageFqName)) {
        val designationPath = FirElementFinder.collectDesignationPath(
            firFile = file,
            declarationContainerClassId = containingClassId,
            targetMemberDeclaration = target,
        )

        if (designationPath != null) {
            return designationPath
        }
    }

    val fallbackClassPath = containingClassId?.let { collectDesignationPathWithContainingClassFallback(target, it) }.orEmpty()
    val fallbackFile = providedFile ?: fallbackClassPath.lastOrNull()?.getContainingFile() ?: file
    val fallbackScript = fallbackFile?.declarations?.singleOrNull() as? FirScript
    val fallbackPath = listOfNotNull(fallbackFile, fallbackScript) + fallbackClassPath
    val patchedPath = patchDesignationPathIfNeeded(target, fallbackPath)
    return FirDesignation(patchedPath, target)
}

/**
 * Whether the search via [FirSymbolProvider] is required to find a declaration in the context of [this] session.
 *
 * Not all sessions have required providers in the session itself (not its dependencies).
 * In such cases, the search might not be able to find even the containing declaration
 */
private val LLFirSession.requiresDependenciesSearch: Boolean
    get() = when (this) {
        is LLFirLibraryOrLibrarySourceResolvableModuleSession -> true
        is LLFirDanglingFileSession -> {
            val module = ktModule as KaDanglingFileModule
            // Dangling files in the ignore self mode have the empty declaration provider,
            // so they cannot find any declarations inside themselves. Search in the context is required
            module.resolutionMode == KaDanglingFileResolutionMode.IGNORE_SELF
        }

        else -> false
    }

private fun collectDesignationPathWithContainingClassFallback(
    target: FirDeclaration,
    containingClassId: ClassId,
): List<FirDeclaration> {
    val useSiteSession by lazy(LazyThreadSafetyMode.NONE) { getTargetSession(target) }

    fun resolveChunk(classId: ClassId): FirRegularClass {
        val declaration = if (useSiteSession.requiresDependenciesSearch) {
            useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.fir
        } else {
            useSiteSession.firProvider.getFirClassifierByFqName(classId)
                ?: useSiteSession.nullableJavaSymbolProvider?.getClassLikeSymbolByClassId(classId)?.fir
                ?: findKotlinStdlibClass(classId, target)
        }

        checkWithAttachment(
            declaration is FirRegularClass,
            message = {
                "'${FirRegularClass::class.simpleName}' expected as a containing declaration, " +
                        "got '${declaration?.javaClass?.simpleName}'. " +
                        "Module: ${useSiteSession.ktModule::class.simpleName}"
            },
            buildAttachment = {
                withEntry("chunk", "$classId in $containingClassId")
                withFirEntry("target", target)
                if (declaration != null) {
                    withFirEntry("foundDeclaration", declaration)
                }
            }
        )

        return declaration
    }

    val containingClassIds = generateSequence(containingClassId) { it.outerClassId }
    val (_, containingClasses) = containingClassIds.fold(target to SmartList<FirRegularClass>()) { (declaration, result), classId ->
        // Psi-based calculator is called explicitly to avoid `LLFirProvider#getContainingClassSymbol`
        // since we have a fallback logic with strict checking (no dependencies in the search scope)
        val psiBasedContainingClass = LLContainingClassCalculator.getContainingClassSymbol(declaration.symbol)?.fir
        checkWithAttachment(
            psiBasedContainingClass == null || psiBasedContainingClass is FirRegularClass,
            message = {
                "${LLContainingClassCalculator::class.simpleName} is supposed to return '${FirRegularClass::class.simpleName}' " +
                        "as a containing declaration since the class is not local (classId exists), got '${psiBasedContainingClass?.javaClass?.simpleName}'. " +
                        "Module: ${useSiteSession.ktModule::class.simpleName}"
            },
        ) {
            withEntry("classId", classId.toString())
            withEntry("containingClassId", containingClassId.toString())
            withFirEntry("declaration", declaration)
        }

        if (psiBasedContainingClass == null && classId.shortClassName.isSpecial) {
            errorWithAttachment(
                "Special classes are supposed to be covered via ${LLContainingClassCalculator::class.simpleName}. " +
                        "Module: ${useSiteSession.ktModule::class.simpleName}"
            ) {
                withEntry("classId", classId.toString())
                withEntry("containingClassId", containingClassId.toString())
                withFirEntry("declaration", declaration)
            }
        }

        val containingClass = psiBasedContainingClass ?: resolveChunk(classId)
        result += containingClass
        containingClass to result
    }

    return containingClasses.asReversed()
}

private fun getTargetSession(target: FirDeclaration): LLFirSession {
    if (target is FirCallableDeclaration) {
        val containingSymbol = target.getContainingClassSymbol()
        if (containingSymbol != null) {
            // Synthetic declarations might have a call site session
            return containingSymbol.llFirSession
        }
    }

    return target.llFirSession
}

private fun findKotlinStdlibClass(classId: ClassId, target: FirDeclaration): FirRegularClass? {
    if (!classId.packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)) {
        return null
    }

    val firFile = target.getContainingFile() ?: return null
    return FirElementFinder.findClassifierWithClassId(firFile, classId) as? FirRegularClass
}

/**
 * Consider using this function only if [collectDesignation] is not applicable.
 *
 * This extension function can be used in the case there your [FirElementWithResolveState] probably
 * doesn't have [getContainingFile] and it doesn't matter for your purposes.
 * Potentially, this function can become obsolete if we support all possible cases in [getContainingFile]
 *
 * @return [FirDesignation] where [FirDesignation.fileOrNull] can be null or throws an exception.
 *
 * @see collectDesignation
 * @see tryCollectDesignation
 * @see tryCollectDesignationWithOptionalFile
 */
fun FirElementWithResolveState.collectDesignationWithOptionalFile(providedFile: FirFile? = null): FirDesignation =
    tryCollectDesignationWithOptionalFile(providedFile) ?: errorWithAttachment("No designation of local declaration") {
        providedFile?.let { withFirEntry("firFile", it) }
    }

/**
 * @return [FirDesignation] where [FirDesignation.fileOrNull] not null or throws an exception.
 *
 * @see collectDesignationWithOptionalFile
 * @see tryCollectDesignation
 * @see tryCollectDesignationWithOptionalFile
 */
fun FirElementWithResolveState.collectDesignation(providedFile: FirFile? = null): FirDesignation =
    tryCollectDesignation(providedFile) ?: errorWithAttachment("No designation of local declaration") {
        withFirEntry("FirDeclaration", this@collectDesignation)
    }

/**
 * Consider using this function only if [tryCollectDesignation] is not applicable.
 *
 * This extension function can be used in the case there your [FirElementWithResolveState] probably
 * doesn't have [getContainingFile] and it doesn't matter for your purposes.
 * Potentially, this function can become obsolete if we support all possible cases in [getContainingFile]
 *
 * @return [FirDesignation] where [FirDesignation.fileOrNull] can be null or null.
 *
 * @see collectDesignationWithOptionalFile
 * @see collectDesignation
 * @see tryCollectDesignation
 */
fun FirElementWithResolveState.tryCollectDesignationWithOptionalFile(providedFile: FirFile? = null): FirDesignation? =
    tryCollectDesignation(providedFile = providedFile, target = this)

/**
 * @return [FirDesignation] with not-null [FirDesignation.file] or null.
 *
 * @see collectDesignation
 * @see tryCollectDesignationWithOptionalFile
 * @see collectDesignationWithOptionalFile
 */
fun FirElementWithResolveState.tryCollectDesignation(providedFile: FirFile? = null): FirDesignation? {
    val designation = tryCollectDesignation(providedFile = providedFile, target = this)
    return designation?.takeIf { it.fileOrNull != null }
}

internal fun patchDesignationPathIfNeeded(target: FirElementWithResolveState, targetPath: List<FirDeclaration>): List<FirDeclaration> {
    return patchDesignationPathForCopy(target, targetPath) ?: targetPath
}

private fun patchDesignationPathForCopy(target: FirElementWithResolveState, targetPath: List<FirDeclaration>): List<FirDeclaration>? {
    val targetModule = target.llFirModuleData.ktModule

    if (targetModule is KaDanglingFileModule && targetModule.resolutionMode == KaDanglingFileResolutionMode.IGNORE_SELF) {
        val targetPsiFile = targetModule.files.singleOrNull() ?: return targetPath

        val contextModule = targetModule.contextModule
        val contextResolutionFacade = contextModule.getResolutionFacade(contextModule.project)

        return buildList {
            for (targetPathDeclaration in targetPath) {
                val targetPathPsi = targetPathDeclaration.psi ?: return null
                if (targetPathPsi !is KtClassOrObject && targetPathPsi !is KtScript && targetPathPsi !is KtFile) return null

                val originalPathPsi = targetPathPsi.unwrapCopy(targetPsiFile) ?: return null
                val originalPathDeclaration = when (originalPathPsi) {
                    is KtClassOrObject -> originalPathPsi.resolveToFirSymbolOfTypeSafe<FirRegularClassSymbol>(contextResolutionFacade)?.fir
                    is KtScript -> originalPathPsi.resolveToFirSymbolOfTypeSafe<FirScriptSymbol>(contextResolutionFacade)?.fir
                    is KtFile -> originalPathPsi.getOrBuildFirFile(contextResolutionFacade)
                    else -> null
                } ?: return null

                add(originalPathDeclaration)
            }
        }
    }

    return targetPath
}
