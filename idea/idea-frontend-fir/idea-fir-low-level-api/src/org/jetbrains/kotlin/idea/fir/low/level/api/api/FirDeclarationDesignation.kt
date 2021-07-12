/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.idea.fir.low.level.api.util.getContainingFile

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
                is FirSimpleFunction, is FirProperty, is FirField, is FirConstructor -> {
                    val klass = declaration.containingClass() ?: return emptyList()
                    if (klass.classId.isLocal) return null
                    klass.toFirRegularClass(declaration.moduleData.session)
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

    require(containingClass is FirRegularClass) {
        "FirRegularClass as containing declaration expected but found ${containingClass.renderWithType()}"
    }
    return if (!containingClass.isLocal) containingClass.collectForNonLocal().asReversed() else null
}

fun FirDeclaration.collectDesignation(firFile: FirFile): FirDeclarationDesignationWithFile =
    tryCollectDesignation(firFile) ?: error("No designation of local declaration ${this.render()}")

fun FirDeclaration.collectDesignation(): FirDeclarationDesignation =
    tryCollectDesignation() ?: error("No designation of local declaration ${this.render()}")

fun FirDeclaration.collectDesignationWithFile(): FirDeclarationDesignationWithFile =
    tryCollectDesignationWithFile() ?: error("No designation of local declaration ${this.render()}")

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
