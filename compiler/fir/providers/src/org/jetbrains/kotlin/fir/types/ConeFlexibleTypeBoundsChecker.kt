/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds.Collections.baseCollectionToMutableEquivalent
import org.jetbrains.kotlin.name.StandardClassIds.Collections.mutableCollectionToBaseCollection

object ConeFlexibleTypeBoundsChecker {
    fun areTypesMayBeLowerAndUpperBoundsOfSameFlexibleTypeByMutability(a: ConeKotlinType, b: ConeKotlinType): Boolean {
        val classId = a.classId ?: return false
        val possiblePairBound = (baseCollectionToMutableEquivalent[classId] ?: mutableCollectionToBaseCollection[classId]) ?: return false

        return possiblePairBound == b.classId
    }

    // We consider base bounds as not mutable collections
    fun getBaseBoundFqNameByMutability(a: ConeKotlinType): ClassId? {
        val classId = a.classId ?: return null

        if (classId in baseCollectionToMutableEquivalent) return classId

        return mutableCollectionToBaseCollection[classId]
    }
}
