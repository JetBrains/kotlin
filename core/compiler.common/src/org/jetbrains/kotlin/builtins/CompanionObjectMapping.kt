/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object CompanionObjectMapping {
    private val fqNamesWithOperatorOf: Set<FqName> = buildSet {
        StandardNames.FqNames.run {
            add(list)
            add(mutableList)
            add(set)
            add(mutableSet)
            add(array.toSafe())
        }
        addAll(PrimitiveType.entries.map(StandardNames::getPrimitiveArrayFqName))
    }

    val classIds: Set<ClassId> = (
            PrimitiveType.entries.map(StandardNames::getPrimitiveFqName) +
                    StandardNames.FqNames.string.toSafe() +
                    StandardNames.FqNames._enum.toSafe() +
                    fqNamesWithOperatorOf
            ).mapTo(linkedSetOf(), ClassId::topLevel)

    fun allClassesWithIntrinsicCompanions(): Set<ClassId> = classIds
}
