/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.constants

import org.jetbrains.kotlin.name.ClassId

data class ClassLiteralValue(val classId: ClassId, val arrayNestedness: Int) {
    override fun toString(): String = buildString {
        repeat(arrayNestedness) { append("kotlin/Array<") }
        append(classId)
        repeat(arrayNestedness) { append(">") }
    }
}
