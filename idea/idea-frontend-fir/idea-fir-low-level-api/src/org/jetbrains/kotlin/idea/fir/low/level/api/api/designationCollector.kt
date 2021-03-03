/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.fir.containingClassForLocal
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.classId
import org.jetbrains.kotlin.fir.declarations.isLocal
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass

fun FirRegularClass.collectDesignation(): List<FirClassLikeDeclaration<*>> {
    val designation = mutableListOf<FirClassLikeDeclaration<*>>()
    designation.add(this)

    fun FirRegularClass.collectForNonLocal() {
        require(!isLocal)
        val firProvider = session.firProvider
        var containingClassId = classId.outerClassId
        while (containingClassId != null) {
            val currentClass = firProvider.getFirClassifierByFqName(containingClassId) ?: break
            designation.add(currentClass)
            containingClassId = containingClassId.outerClassId
        }
    }

    fun FirRegularClass.collectForLocal() {
        require(isLocal)
        var containingClassLookUp = containingClassForLocal()
        while (containingClassLookUp != null && containingClassLookUp.classId.isLocal) {
            val currentClass = containingClassLookUp.toFirRegularClass(session) ?: break
            designation.add(currentClass)
            containingClassLookUp = currentClass.containingClassForLocal()
        }
    }

    if (isLocal) collectForLocal() else collectForNonLocal()
    designation.reverse()
    return designation
}