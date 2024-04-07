/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.nullableJavaSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryOrLibrarySourceResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.containingClassIdOrNull
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isLocalForLazyResolutionPurposes
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.unwrapCopy
import org.jetbrains.kotlin.analysis.project.structure.DanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
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
    when (target) {
        is FirSyntheticProperty, is FirSyntheticPropertyAccessor -> return null
        is FirSimpleFunction,
        is FirProperty,
        is FirField,
        is FirConstructor,
        is FirEnumEntry,
        is FirPropertyAccessor,
        is FirErrorProperty,
        -> {
            requireIsInstance<FirCallableDeclaration>(target)

            // We shouldn't try to build a designation path for such fake declarations as they
            // do not depend on outer classes during resolution
            if (target.isCopyCreatedInScope) return FirDesignation(target)

            if (target.symbol.isLocalForLazyResolutionPurposes) {
                return null
            }

            val containingClassId = target.containingClassLookupTag()?.classId
            return collectDesignationPathWithContainingClass(providedFile, target, containingClassId)
        }

        is FirClassLikeDeclaration -> {
            if (target.isLocal) {
                return null
            }

            val containingClassId = target.symbol.classId.outerClassId
            return collectDesignationPathWithContainingClass(providedFile, target, containingClassId)
        }

        is FirDanglingModifierList -> {
            val containingClassId = target.containingClass()?.classId
            return collectDesignationPathWithContainingClass(providedFile, target, containingClassId)
        }

        is FirAnonymousInitializer -> {
            val containingClassId = target.containingClassIdOrNull()
            return collectDesignationPathWithContainingClass(providedFile, target, containingClassId)
        }

        is FirFile -> return FirDesignation(target)
        is FirScript, is FirCodeFragment -> {
            requireIsInstance<FirDeclaration>(target)

            return collectDesignationPathWithContainingClass(providedFile, target, containingClassId = null)
        }

        else -> return null
    }
}

private fun collectDesignationPathWithContainingClass(
    providedFile: FirFile?,
    target: FirDeclaration,
    containingClassId: ClassId?,
): FirDesignation? {
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

    val fallbackClassPath = collectDesignationPathWithContainingClassFallback(target, containingClassId)
    val fallbackFile = providedFile ?: fallbackClassPath?.lastOrNull()?.getContainingFile() ?: file
    val fallbackScript = fallbackFile?.declarations?.singleOrNull() as? FirScript
    val fallbackPath = listOfNotNull(fallbackFile, fallbackScript) + fallbackClassPath.orEmpty()
    val patchedPath = patchDesignationPathIfNeeded(target, fallbackPath)
    return FirDesignation(patchedPath, target)
}

private fun collectDesignationPathWithContainingClassFallback(
    target: FirDeclaration,
    containingClassId: ClassId?,
): List<FirDeclaration>? {
    val useSiteSession = getTargetSession(target)

    fun resolveChunk(classId: ClassId): FirRegularClass {
        val declaration = if (useSiteSession is LLFirLibraryOrLibrarySourceResolvableModuleSession) {
            useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.fir
        } else {
            useSiteSession.firProvider.getFirClassifierByFqName(classId)
                ?: useSiteSession.nullableJavaSymbolProvider?.getClassLikeSymbolByClassId(classId)?.fir
                ?: findKotlinStdlibClass(classId, target)
        }

        checkWithAttachment(
            declaration is FirRegularClass,
            message = { "'FirRegularClass' expected as a containing declaration, got '${declaration?.javaClass?.name}'" },
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

    val chunks = generateSequence(containingClassId) { it.outerClassId }.toList()

    if (chunks.any { it.shortClassName.isSpecial }) {
        val fallbackResult = collectDesignationPathWithTreeTraversal(target)
        if (fallbackResult != null) {
            return fallbackResult
        }
    }

    val result = chunks
        .dropWhile { it.shortClassName.isSpecial }
        .map { resolveChunk(it) }
        .asReversed()

    return result
}

/*
    This implementation is certainly inefficient, however there seem to be no better way to implement designation collection for
    anonymous outer classes unless FIR tree gets a way to get an element parent.
 */
private fun collectDesignationPathWithTreeTraversal(target: FirDeclaration): List<FirRegularClass>? {
    val containingFile = target.getContainingFile() ?: return null

    val path = mutableListOf<FirRegularClass>()
    var result: List<FirRegularClass>? = null

    val visitor = object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (result != null) {
                return
            } else if (element === target) {
                result = path
            } else {
                try {
                    if (element is FirRegularClass) {
                        path += element
                    }

                    element.acceptChildren(this)
                } finally {
                    if (element is FirRegularClass) {
                        path.removeLast()
                    }
                }
            }
        }
    }

    containingFile.accept(visitor)
    return result
}

private fun getTargetSession(target: FirDeclaration): FirSession {
    if (target is FirCallableDeclaration) {
        val containingSymbol = target.containingClassLookupTag()?.toSymbol(target.moduleData.session)
        if (containingSymbol != null) {
            // Synthetic declarations might have a call site session
            return containingSymbol.moduleData.session
        }
    }

    return target.moduleData.session
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
fun FirElementWithResolveState.tryCollectDesignation(providedFile: FirFile? = null): FirDesignation? = when (this) {
    is FirSyntheticProperty, is FirSyntheticPropertyAccessor -> unexpectedElementError<FirElementWithResolveState>(this)
    is FirDeclaration -> {
        val designation = tryCollectDesignation(providedFile = providedFile, target = this)
        designation?.takeIf { it.fileOrNull != null }
    }
    else -> unexpectedElementError<FirElementWithResolveState>(this)
}

internal fun patchDesignationPathIfNeeded(target: FirElementWithResolveState, targetPath: List<FirDeclaration>): List<FirDeclaration> {
    return patchDesignationPathForCopy(target, targetPath) ?: targetPath
}

private fun patchDesignationPathForCopy(target: FirElementWithResolveState, targetPath: List<FirDeclaration>): List<FirDeclaration>? {
    val targetModule = target.llFirModuleData.ktModule

    if (targetModule is KtDanglingFileModule && targetModule.resolutionMode == DanglingFileResolutionMode.IGNORE_SELF) {
        val targetPsiFile = targetModule.file

        val contextModule = targetModule.contextModule
        val contextResolveSession = contextModule.getFirResolveSession(contextModule.project)

        return buildList {
            for (targetPathDeclaration in targetPath) {
                val targetPathPsi = targetPathDeclaration.psi ?: return null
                if (targetPathPsi !is KtClassOrObject && targetPathPsi !is KtScript && targetPathPsi !is KtFile) return null

                val originalPathPsi = targetPathPsi.unwrapCopy(targetPsiFile) ?: return null
                val originalPathDeclaration = when (originalPathPsi) {
                    is KtClassOrObject -> originalPathPsi.getOrBuildFirSafe<FirRegularClass>(contextResolveSession)
                    is KtScript -> originalPathPsi.getOrBuildFirSafe<FirScript>(contextResolveSession)
                    is KtFile -> originalPathPsi.getOrBuildFirFile(contextResolveSession)
                    else -> null
                } ?: return null

                add(originalPathDeclaration)
            }
        }
    }

    return targetPath
}