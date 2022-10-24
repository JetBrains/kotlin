/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLookupTagWithFixedSymbol
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder

class FirDeclarationDesignationWithFile(
    path: List<FirDeclaration>,
    declaration: FirDeclaration,
    val firFile: FirFile
) : FirDeclarationDesignation(
    path,
    declaration,
) {
    fun toSequenceWithFile(includeTarget: Boolean): Sequence<FirDeclaration> = sequence {
        yield(firFile)
        yieldAll(path)
        if (includeTarget) yield(declaration)
    }
}

open class FirDeclarationDesignation(
    val path: List<FirDeclaration>,
    val declaration: FirDeclaration,
) {
    fun toSequence(includeTarget: Boolean): Sequence<FirDeclaration> = sequence {
        yieldAll(path)
        if (includeTarget) yield(declaration)
    }
}

private fun FirRegularClass.collectForNonLocal(): List<FirDeclaration> {
    require(!isLocal)
    val firProvider = moduleData.session.firProvider
    var containingClassId = classId.outerClassId
    val designation = mutableListOf<FirDeclaration>(this)
    while (containingClassId != null) {
        val currentClass = firProvider.getFirClassifierByFqName(containingClassId) ?: break
        designation.add(currentClass)
        containingClassId = containingClassId.outerClassId
    }
    return designation
}

private fun collectDesignationPath(declaration: FirDeclaration): List<FirDeclaration>? {
    val containingClass = when (declaration) {
        is FirCallableDeclaration -> {
            if (declaration.symbol.callableId.isLocal) return null
            if ((declaration as? FirCallableDeclaration)?.status?.visibility == Visibilities.Local) return null
            when (declaration) {
                is FirSimpleFunction, is FirProperty, is FirField, is FirConstructor, is FirEnumEntry -> {
                    val klass = declaration.containingClassLookupTag() ?: return emptyList()
                    if (klass.classId.isLocal) return null
                    klass.toFirRegularClassFromSameSession(declaration.moduleData.session)
                }
                else -> return null
            }
        }
        is FirClassLikeDeclaration -> {
            if (declaration.isLocal) return null
            declaration.symbol.classId.outerClassId?.let(declaration.moduleData.session.firProvider::getFirClassifierByFqName)
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

fun FirDeclaration.collectDesignation(firFile: FirFile): FirDeclarationDesignationWithFile =
    tryCollectDesignation(firFile) ?: buildErrorWithAttachment("No designation of local declaration") {
        withFirEntry("firFile", firFile)
    }

fun FirDeclaration.collectDesignation(): FirDeclarationDesignation =
    tryCollectDesignation()
        ?: buildErrorWithAttachment("No designation of local declaration") {
            withFirEntry("FirDeclaration", this@collectDesignation)
        }

fun FirDeclaration.collectDesignationWithFile(): FirDeclarationDesignationWithFile =
    tryCollectDesignationWithFile()
        ?: buildErrorWithAttachment("No designation of local declaration") {
            withFirEntry("FirDeclaration", this@collectDesignationWithFile)
        }

fun FirDeclaration.tryCollectDesignation(firFile: FirFile): FirDeclarationDesignationWithFile? =
    collectDesignationPath(this)?.let {
        FirDeclarationDesignationWithFile(it, this, firFile)
    }

fun FirDeclaration.tryCollectDesignation(): FirDeclarationDesignation? =
    collectDesignationPath(this)?.let {
        FirDeclarationDesignation(it, this)
    }

fun FirDeclaration.tryCollectDesignationWithFile(): FirDeclarationDesignationWithFile? {
    val path = collectDesignationPath(this) ?: return null
    val firFile = getContainingFile() ?: return null
    return FirDeclarationDesignationWithFile(path, this, firFile)
}
