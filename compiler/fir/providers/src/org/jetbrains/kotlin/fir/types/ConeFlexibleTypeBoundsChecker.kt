/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

object ConeFlexibleTypeBoundsChecker {
    private val baseTypesToMutableEquivalent = mapOf(
        StandardClassIds.Iterable to StandardClassIds.MutableIterable,
        StandardClassIds.Iterator to StandardClassIds.MutableIterator,
        StandardClassIds.ListIterator to StandardClassIds.MutableListIterator,
        StandardClassIds.List to StandardClassIds.MutableList,
        StandardClassIds.Collection to StandardClassIds.MutableCollection,
        StandardClassIds.Set to StandardClassIds.MutableSet,
        StandardClassIds.Map to StandardClassIds.MutableMap,
        StandardClassIds.MapEntry to StandardClassIds.MutableMapEntry
    )
    private val mutableToBaseMap = baseTypesToMutableEquivalent.entries.associateBy({ it.value }) { it.key }

    fun areTypesMayBeLowerAndUpperBoundsOfSameFlexibleTypeByMutability(a: ConeKotlinType, b: ConeKotlinType): Boolean {
        val classId = a.classId ?: return false
        val possiblePairBound = (baseTypesToMutableEquivalent[classId] ?: mutableToBaseMap[classId]) ?: return false

        return possiblePairBound == b.classId
    }

    // We consider base bounds as not mutable collections
    fun getBaseBoundFqNameByMutability(a: ConeKotlinType): ClassId? {
        val classId = a.classId ?: return null

        if (classId in baseTypesToMutableEquivalent) return classId

        return mutableToBaseMap[classId]
    }
}
