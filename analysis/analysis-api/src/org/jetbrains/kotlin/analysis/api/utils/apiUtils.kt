/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.utils

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

private val implementationPackageNames = listOf(
    "org.jetbrains.kotlin.analysis.api.impl.base",
    "org.jetbrains.kotlin.analysis.api.fir",
    "org.jetbrains.kotlin.analysis.api.descriptors",
)

@KaImplementationDetail
public fun getApiKClassOf(value: Any): KClass<*> {
    fun KClass<*>.isImplementationIndependent(): Boolean {
        if (this == Any::class) return false
        val qualifiedName = qualifiedName ?: return false
        return implementationPackageNames.none { qualifiedName.startsWith("$it.") }
    }

    val valueClass = value::class
    val allClasses = listOf(valueClass) + valueClass.allSuperclasses

    val matchingClasses = allClasses.filter { it.isImplementationIndependent() }
    val matchingClassSet = matchingClasses.toSet()

    val matchingClassesRanking = matchingClasses
        .associateWith { matchingClassSet.intersect(it.allSuperclasses).size }

    // Find supertypes with the highest number of frontend-independent supertypes
    // It means more specific classes will be selected (such as KaClassSymbol instead of KaSymbol)
    val minSupertypeCount = matchingClassesRanking.maxOf { it.value }

    // If there are multiple matching classes, at least choose some stable one (based on the simple name ordering)
    return matchingClassesRanking
        .filter { it.value == minSupertypeCount }
        .keys
        .sortedBy { it.simpleName }
        .first()
}
