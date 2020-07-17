/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.metadata.serialization.StringTable
import org.jetbrains.kotlin.name.ClassId

interface FirElementAwareStringTable : StringTable {
    fun getQualifiedClassNameIndex(classId: ClassId): Int =
        getQualifiedClassNameIndex(classId.asString(), classId.isLocal)

    fun getFqNameIndex(classLikeDeclaration: FirClassLikeDeclaration<*>): Int {
        val classId = classLikeDeclaration.symbol.classId.takeIf { !it.isLocal }
            ?: getLocalClassIdReplacement(classLikeDeclaration)
            ?: throw IllegalStateException("Cannot get FQ name of local class: ${classLikeDeclaration.render()}")

        return getQualifiedClassNameIndex(classId)
    }

    fun getLocalClassIdReplacement(classLikeDeclaration: FirClassLikeDeclaration<*>): ClassId? = null
}
