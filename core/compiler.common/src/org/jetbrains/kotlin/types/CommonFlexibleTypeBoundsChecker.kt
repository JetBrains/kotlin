/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.name.FqName

object CommonFlexibleTypeBoundsChecker {
    val baseTypesToMutableEquivalent = mapOf(
        FqNames.iterable to FqNames.mutableIterable,
        FqNames.iterator to FqNames.mutableIterator,
        FqNames.listIterator to FqNames.mutableListIterator,
        FqNames.list to FqNames.mutableList,
        FqNames.collection to FqNames.mutableCollection,
        FqNames.set to FqNames.mutableSet,
        FqNames.map to FqNames.mutableMap,
        FqNames.mapEntry to FqNames.mutableMapEntry
    )
    val mutableToBaseMap = baseTypesToMutableEquivalent.entries.associateBy({ it.value }) { it.key }

    fun getBaseBoundFqNameByMutability(fqName: FqName): FqName? =
        if (fqName in baseTypesToMutableEquivalent) fqName
        else mutableToBaseMap[fqName]
}
