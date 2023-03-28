/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.nullableJavaSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryOrLibrarySourceResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.diagnostics.ConeDestructuringDeclarationsOnTopLevel
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId

class FirDesignationWithFile(
    path: List<FirRegularClass>,
    target: FirElementWithResolveState,
    val firFile: FirFile
) : FirDesignation(
    path,
    target,
) {
    fun toSequenceWithFile(includeTarget: Boolean): Sequence<FirElementWithResolveState> = sequence {
        yield(firFile)
        yieldAll(path)
        if (includeTarget) yield(target)
    }
}

open class FirDesignation(
    val path: List<FirRegularClass>,
    val target: FirElementWithResolveState,
) {
    val firstNonFileDeclaration: FirElementWithResolveState
        get() = path.firstOrNull() ?: target

    fun toSequence(includeTarget: Boolean): Sequence<FirElementWithResolveState> = sequence {
        yieldAll(path)
        if (includeTarget) yield(target)
    }
}

private fun collectDesignationPath(target: FirElementWithResolveState): List<FirRegularClass>? {
    when (target) {
        is FirSimpleFunction,
        is FirProperty,
        is FirField,
        is FirConstructor,
        is FirEnumEntry,
        is FirPropertyAccessor -> {
            require(target is FirCallableDeclaration)

            if ((target !is FirConstructor && target.symbol.callableId.isLocal) || target.status.visibility == Visibilities.Local) {
                return null
            }

            val containingClassId = target.containingClassLookupTag()?.classId ?: return emptyList()

            if (target.origin == FirDeclarationOrigin.SubstitutionOverride) {
                val originalContainingClassId = target.originalForSubstitutionOverride?.containingClassLookupTag()?.classId
                if (containingClassId == originalContainingClassId) {
                    // Ugly temporary hack for call-site substitution overrides (KTIJ-24004).
                    // Containing class ID from the origin cannot be used, as the origin might be in a different module.
                    return emptyList()
                }
            }

            return collectDesignationPathWithContainingClass(target, containingClassId)
        }

        is FirClassLikeDeclaration -> {
            if (target.isLocal) {
                return null
            }

            val containingClassId = target.symbol.classId.outerClassId ?: return emptyList()
            return collectDesignationPathWithContainingClass(target, containingClassId)
        }

        is FirDanglingModifierList -> {
            val containingClassId = target.containingClass()?.classId ?: return emptyList()
            return collectDesignationPathWithContainingClass(target, containingClassId)
        }

        is FirErrorProperty -> {
            return if (target.diagnostic == ConeDestructuringDeclarationsOnTopLevel) emptyList() else null
        }

        is FirScript -> {
            return emptyList()
        }

        else -> {
            return null
        }
    }
}

private fun collectDesignationPathWithContainingClass(target: FirDeclaration, containingClassId: ClassId): List<FirRegularClass>? {
    if (containingClassId.isLocal) {
        return null
    }

    val useSiteSession = getTargetSession(target)

    fun resolveChunk(classId: ClassId): FirRegularClass {
        val declaration = if (useSiteSession is LLFirLibraryOrLibrarySourceResolvableModuleSession) {
            useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.fir
        } else {
            useSiteSession.firProvider.getFirClassifierByFqName(classId)
                ?: useSiteSession.nullableJavaSymbolProvider?.getClassLikeSymbolByClassId(classId)?.fir
                ?: findKotlinStdlibClass(classId, target)
        }

        checkWithAttachmentBuilder(
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

    return chunks
        .dropWhile { it.shortClassName.isSpecial }
        .map { resolveChunk(it) }
        .asReversed()
}

/*
    This implementation is certainly inefficient, however there seem to be no better way to implement designation collection for
    anonymous outer classes unless FIR tree gets a way to get an element parent.
 */
private fun collectDesignationPathWithTreeTraversal(target: FirDeclaration): List<FirRegularClass>? {
    val containingFile = target.getContainingFile() ?: return null

    val path = ArrayDeque<FirElement>()
    path.addLast(containingFile)

    var result: List<FirRegularClass>? = null

    val visitor = object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (result != null) {
                return
            } else if (element === target) {
                result = path.filterIsInstance<FirRegularClass>()
            } else {
                try {
                    path.addLast(element)
                    element.acceptChildren(this)
                } finally {
                    path.removeLast()
                }
            }
        }
    }

    containingFile.accept(visitor)
    return result
}

private fun getTargetSession(target: FirDeclaration): FirSession {
    if (target is FirSyntheticProperty) {
        return getTargetSession(target.getter)
    }

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

fun FirElementWithResolveState.collectDesignation(firFile: FirFile): FirDesignationWithFile =
    tryCollectDesignation(firFile) ?: buildErrorWithAttachment("No designation of local declaration") {
        withFirEntry("firFile", firFile)
    }

fun FirElementWithResolveState.collectDesignation(): FirDesignation =
    tryCollectDesignation()
        ?: buildErrorWithAttachment("No designation of local declaration") {
            withFirEntry("FirDeclaration", this@collectDesignation)
        }

fun FirElementWithResolveState.collectDesignationWithFile(): FirDesignationWithFile =
    tryCollectDesignationWithFile()
        ?: buildErrorWithAttachment("No designation of local declaration") {
            withFirEntry("FirDeclaration", this@collectDesignationWithFile)
        }

fun FirElementWithResolveState.tryCollectDesignation(firFile: FirFile): FirDesignationWithFile? =
    collectDesignationPath(this)?.let {
        FirDesignationWithFile(it, this, firFile)
    }

fun FirElementWithResolveState.tryCollectDesignation(): FirDesignation? =
    collectDesignationPath(this)?.let {
        FirDesignation(it, this)
    }

fun FirElementWithResolveState.tryCollectDesignationWithFile(): FirDesignationWithFile? {
    return when (this) {
        is FirScript, is FirFileAnnotationsContainer -> {
            val firFile = getContainingFile() ?: return null
            FirDesignationWithFile(path = emptyList(), this, firFile)
        }
        is FirDeclaration -> {
            val path = collectDesignationPath(this) ?: return null
            val firFile = getContainingFile() ?: return null
            FirDesignationWithFile(path, this, firFile)
        }
        else -> unexpectedElementError<FirElementWithResolveState>(this)
    }
}
