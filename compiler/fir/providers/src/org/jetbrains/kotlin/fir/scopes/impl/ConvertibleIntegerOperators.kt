/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object ConvertibleIntegerOperators {
    val operatorsNames: Set<Name> = listOf(
        "plus", "minus", "times", "div", "rem",
        "and", "or", "xor",
        "shl", "shr", "ushr"
    ).mapTo(mutableSetOf()) { Name.identifier(it) }
}
