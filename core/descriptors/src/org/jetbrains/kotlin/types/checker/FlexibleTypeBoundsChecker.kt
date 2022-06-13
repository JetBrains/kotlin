/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

object FlexibleTypeBoundsChecker {
    private val fqNames = StandardNames.FqNames
    private val baseTypesToMutableEquivalent = mapOf(
        fqNames.iterable to fqNames.mutableIterable,
        fqNames.iterator to fqNames.mutableIterator,
        fqNames.listIterator to fqNames.mutableListIterator,
        fqNames.list to fqNames.mutableList,
        fqNames.collection to fqNames.mutableCollection,
        fqNames.set to fqNames.mutableSet,
        fqNames.map to fqNames.mutableMap,
        fqNames.mapEntry to fqNames.mutableMapEntry
    )
    private val mutableToBaseMap = baseTypesToMutableEquivalent.entries.associateBy({ it.value }) { it.key }

    fun areTypesMayBeLowerAndUpperBoundsOfSameFlexibleTypeByMutability(a: KotlinType, b: KotlinType): Boolean {
        val fqName = a.constructor.declarationDescriptor?.fqNameSafe ?: return false
        val possiblePairBound = (baseTypesToMutableEquivalent[fqName] ?: mutableToBaseMap[fqName]) ?: return false

        return possiblePairBound == b.constructor.declarationDescriptor?.fqNameSafe
    }

    // We consider base bounds as readonly collection interfaces (e.g. kotlin.collections.Iterable).
    fun getBaseBoundFqNameByMutability(type: KotlinType): FqName? =
        type.constructor.declarationDescriptor?.fqNameSafe?.let(::getBaseBoundFqNameByMutability)

    fun getBaseBoundFqNameByMutability(fqName: FqName): FqName? =
        if (fqName in baseTypesToMutableEquivalent) fqName
        else mutableToBaseMap[fqName]
}
