/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.name.ClassId

object CompanionObjectMapping {
    val classIds: Set<ClassId> = (
            PrimitiveType.NUMBER_TYPES.map(StandardNames::getPrimitiveFqName) +
                    StandardNames.FqNames.string.toSafe() +
                    StandardNames.FqNames._boolean.toSafe() +
                    StandardNames.FqNames._enum.toSafe()
            ).mapTo(linkedSetOf(), ClassId::topLevel)

    fun allClassesWithIntrinsicCompanions(): Set<ClassId> = classIds
}
