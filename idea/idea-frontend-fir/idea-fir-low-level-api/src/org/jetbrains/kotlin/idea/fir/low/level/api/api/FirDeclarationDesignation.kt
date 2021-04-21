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
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider

class FirDeclarationDesignation(
    val path: List<FirDeclaration>,
    val declaration: FirDeclaration,
    val isLocalDesignation: Boolean,
) {
    @OptIn(ExperimentalStdlibApi::class)
    val fullDesignation = buildList {
        addAll(path)
        add(declaration)
    }
}


object DeclarationDesignationCollector {
    fun collectDesignation(declaration: FirDeclaration): FirDeclarationDesignation {
        val firProvider = declaration.declarationSiteSession.firProvider

        val containingClass = when (declaration) {
            is FirCallableDeclaration<*> -> declaration.containingClass()?.toFirRegularClass(declaration.declarationSiteSession)
            is FirClassLikeDeclaration<*> -> declaration.symbol.classId.outerClassId?.let(firProvider::getFirClassifierByFqName)
            else -> error("Invalid declaration ${declaration.renderWithType()}")
        } ?: return FirDeclarationDesignation(emptyList(), declaration, isLocalDesignation = false)

        require(containingClass is FirRegularClass) {
            "FirRegularClass as containing declaration expected but found ${containingClass.renderWithType()}"
        }

        val path = when {
            containingClass.isLocal -> containingClass.collectForLocal()
            else -> containingClass.collectForNonLocal()
        }

        return FirDeclarationDesignation(
            path.reversed(),
            declaration,
            isLocalDesignation = false
        )
    }

    private fun FirRegularClass.collectForNonLocal(): List<FirClassLikeDeclaration<*>> {
        require(!isLocal)
        val firProvider = declarationSiteSession.firProvider
        var containingClassId = classId.outerClassId
        val designation = mutableListOf<FirClassLikeDeclaration<*>>(this)
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
            val currentClass = containingClassLookUp.toFirRegularClass(declarationSiteSession) ?: break
            designation.add(currentClass)
            containingClassLookUp = currentClass.containingClassForLocal()
        }
        return designation
    }
}


fun FirDeclaration.collectDesignation(): FirDeclarationDesignation =
    DeclarationDesignationCollector.collectDesignation(this)
