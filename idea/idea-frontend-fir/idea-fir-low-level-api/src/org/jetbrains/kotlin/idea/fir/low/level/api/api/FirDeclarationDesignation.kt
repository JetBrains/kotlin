/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.containingClassForLocal
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass

typealias FirDeclarationUntypedDesignation = FirDeclarationDesignation<FirDeclaration>
typealias FirDeclarationUntypedDesignationWithFile = FirDeclarationDesignationWithFile<FirDeclaration>

class FirDeclarationDesignationWithFile<out T : FirDeclaration>(
    path: List<FirDeclaration>,
    declaration: T,
    isLocalDesignation: Boolean,
    val firFile: FirFile
) : FirDeclarationDesignation<T>(
    path,
    declaration,
    isLocalDesignation
) {
    fun toSequenceWithFile(includeTarget: Boolean): Sequence<FirDeclaration> = sequence {
        yield(firFile)
        yieldAll(path)
        if (includeTarget) yield(declaration)
    }
}

open class FirDeclarationDesignation<out T : FirDeclaration>(
    val path: List<FirDeclaration>,
    val declaration: T,
    val isLocalDesignation: Boolean
) {
    fun toSequence(includeTarget: Boolean): Sequence<FirDeclaration> = sequence {
        yieldAll(path)
        if (includeTarget) yield(declaration)
    }
}

private fun collectDesignationAndIsLocal(declaration: FirDeclaration): Pair<List<FirDeclaration>, Boolean> {
    val firProvider = declaration.moduleData.session.firProvider

    val containingClass = when (declaration) {
        is FirCallableDeclaration<*> -> declaration.containingClass()?.toFirRegularClass(declaration.moduleData.session)
        is FirClassLikeDeclaration<*> -> declaration.symbol.classId.outerClassId?.let(firProvider::getFirClassifierByFqName)
        else -> error("Invalid declaration ${declaration.renderWithType()}")
    } ?: return emptyList<FirDeclaration>() to false

    require(containingClass is FirRegularClass) {
        "FirRegularClass as containing declaration expected but found ${containingClass.renderWithType()}"
    }

    val path = when {
        containingClass.isLocal -> containingClass.collectForLocal()
        else -> containingClass.collectForNonLocal()
    }
    return path.reversed() to containingClass.isLocal
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

private fun FirRegularClass.collectForLocal(): List<FirClassLikeDeclaration<*>> {
    require(isLocal)
    var containingClassLookUp = containingClassForLocal()
    val designation = mutableListOf<FirClassLikeDeclaration<*>>(this)
    while (containingClassLookUp != null && containingClassLookUp.classId.isLocal) {
        val currentClass = containingClassLookUp.toFirRegularClass(moduleData.session) ?: break
        designation.add(currentClass)
        containingClassLookUp = currentClass.containingClassForLocal()
    }
    return designation
}


fun FirDeclaration.collectDesignation(firFile: FirFile): FirDeclarationUntypedDesignationWithFile =
    collectDesignationAndIsLocal(this).let {
        FirDeclarationUntypedDesignationWithFile(it.first, this, it.second, firFile)
    }

fun FirDeclaration.collectDesignation(): FirDeclarationUntypedDesignation =
    collectDesignationAndIsLocal(this).let {
        FirDeclarationUntypedDesignation(it.first, this, it.second)
    }
