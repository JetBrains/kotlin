/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object CompanionObjectMapping {
    private val fqNamesWithOperatorOf: List<FqName>
        get() = buildList {
            StandardNames.FqNames.run {
                add(list)
                add(mutableList)
                add(set)
                add(mutableSet)
                add(array.toSafe())
            }
            addAll(PrimitiveType.entries.map(StandardNames::getPrimitiveArrayFqName))
        }

    private val allFqNames: List<FqName>
        get() = PrimitiveType.entries.map(StandardNames::getPrimitiveFqName) +
                StandardNames.FqNames.string.toSafe() +
                StandardNames.FqNames._enum.toSafe() +
                fqNamesWithOperatorOf

    val classIds: Set<ClassId> = allFqNames.mapTo(linkedSetOf(), ClassId::topLevel)

    val companionClassIds: Set<ClassId> = classIds.mapTo(linkedSetOf()) {
        it.createNestedClassId(Name.identifier("Companion"))
    }

    fun allClassesWithIntrinsicCompanions(): Set<ClassId> = classIds
}
