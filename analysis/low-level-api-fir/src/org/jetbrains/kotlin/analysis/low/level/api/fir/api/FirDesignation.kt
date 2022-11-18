/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElementWithResolvePhase
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.java.javaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLookupTagWithFixedSymbol
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.diagnostics.ConeDestructuringDeclarationsOnTopLevel
import org.jetbrains.kotlin.fir.java.javaSymbolProvider

class FirDesignationWithFile(
    path: List<FirRegularClass>,
    target: FirElementWithResolvePhase,
    val firFile: FirFile
) : FirDesignation(
    path,
    target,
) {
    fun toSequenceWithFile(includeTarget: Boolean): Sequence<FirElementWithResolvePhase> = sequence {
        yield(firFile)
        yieldAll(path)
        if (includeTarget) yield(target)
    }
}

open class FirDesignation(
    val path: List<FirRegularClass>,
    val target: FirElementWithResolvePhase,
) {
    val firstNonFileDeclaration: FirElementWithResolvePhase
        get() = path.firstOrNull() ?: target

    fun toSequence(includeTarget: Boolean): Sequence<FirElementWithResolvePhase> = sequence {
        yieldAll(path)
        if (includeTarget) yield(target)
    }
}

private fun FirRegularClass.collectForNonLocal(): List<FirRegularClass> {
    require(!isLocal)
    val firProvider = moduleData.session.firProvider
    var containingClassId = classId.outerClassId
    val designation = mutableListOf<FirRegularClass>(this)
    while (containingClassId != null) {
        val currentClass = firProvider.getFirClassifierByFqName(containingClassId) as? FirRegularClass ?: break
        designation.add(currentClass)
        containingClassId = containingClassId.outerClassId
    }
    return designation
}

private fun collectDesignationPath(target: FirElementWithResolvePhase): List<FirRegularClass>? {
    val containingClass = when (target) {
        is FirCallableDeclaration -> {
            if (target !is FirConstructor && target.symbol.callableId.isLocal) return null
            if ((target as? FirCallableDeclaration)?.status?.visibility == Visibilities.Local) return null
            when (target) {
                is FirSimpleFunction, is FirProperty, is FirField, is FirConstructor, is FirEnumEntry, is FirPropertyAccessor -> {
                    val klass = target.containingClassLookupTag() ?: return emptyList()
                    if (klass.classId.isLocal) return null
                    klass.toFirRegularClassFromSameSession(target.moduleData.session)
                }
                is FirErrorProperty -> {
                    return if (target.diagnostic == ConeDestructuringDeclarationsOnTopLevel) {
                        emptyList()
                    } else {
                        null
                    }
                }
                else -> return null
            }
        }
        is FirClassLikeDeclaration -> {
            if (target.isLocal) return null
            val outerClassId = target.symbol.classId.outerClassId
            outerClassId?.let(target.moduleData.session.firProvider::getFirClassifierByFqName)
                ?: outerClassId?.let(target.moduleData.session.javaSymbolProvider::getClassLikeSymbolByClassId)?.fir
        }
        is FirDanglingModifierList -> {
            val klass = target.containingClass() ?: return emptyList()
            if (klass.classId.isLocal) return null
            klass.toFirRegularClassFromSameSession(target.moduleData.session)
        }
        else -> return null
    } ?: return emptyList()

    checkWithAttachmentBuilder(containingClass is FirRegularClass, { "FirRegularClass as containing declaration expected" }) {
        withFirEntry("containingClassFir", containingClass)
    }
    return if (!containingClass.isLocal) containingClass.collectForNonLocal().asReversed() else null
}

private fun ConeClassLikeLookupTag.toFirRegularClassFromSameSession(useSiteSession: FirSession): FirRegularClass? {
    if (this is ConeClassLookupTagWithFixedSymbol) return symbol.fir as? FirRegularClass
    return useSiteSession.firProvider.getFirClassifierByFqName(classId) as? FirRegularClass
}

fun FirElementWithResolvePhase.collectDesignation(firFile: FirFile): FirDesignationWithFile =
    tryCollectDesignation(firFile) ?: buildErrorWithAttachment("No designation of local declaration") {
        withFirEntry("firFile", firFile)
    }

fun FirElementWithResolvePhase.collectDesignation(): FirDesignation =
    tryCollectDesignation()
        ?: buildErrorWithAttachment("No designation of local declaration") {
            withFirEntry("FirDeclaration", this@collectDesignation)
        }

fun FirElementWithResolvePhase.collectDesignationWithFile(): FirDesignationWithFile =
    tryCollectDesignationWithFile()
        ?: buildErrorWithAttachment("No designation of local declaration") {
            withFirEntry("FirDeclaration", this@collectDesignationWithFile)
        }

fun FirElementWithResolvePhase.tryCollectDesignation(firFile: FirFile): FirDesignationWithFile? =
    collectDesignationPath(this)?.let {
        FirDesignationWithFile(it, this, firFile)
    }

fun FirElementWithResolvePhase.tryCollectDesignation(): FirDesignation? =
    collectDesignationPath(this)?.let {
        FirDesignation(it, this)
    }

fun FirElementWithResolvePhase.tryCollectDesignationWithFile(): FirDesignationWithFile? {
    return when (this) {
        is FirDeclaration -> {
            val path = collectDesignationPath(this) ?: return null
            val firFile = getContainingFile() ?: return null
            FirDesignationWithFile(path, this, firFile)
        }
        is FirFileAnnotationsContainer -> {
            val firFile = getContainingFile() ?: return null
            FirDesignationWithFile(path = emptyList(), this, firFile)
        }
        else -> unexpectedElementError<FirElementWithResolvePhase>(this)
    }
}
