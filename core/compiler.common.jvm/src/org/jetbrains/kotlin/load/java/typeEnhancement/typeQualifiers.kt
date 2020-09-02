/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.typeEnhancement

enum class NullabilityQualifier {
    NULLABLE,
    NOT_NULL,
    FORCE_FLEXIBILITY
}

enum class MutabilityQualifier {
    READ_ONLY,
    MUTABLE
}

class JavaTypeQualifiers(
    val nullability: NullabilityQualifier?,
    val mutability: MutabilityQualifier?,
    val isNotNullTypeParameter: Boolean,
    val isNullabilityQualifierForWarning: Boolean = false
) {
    companion object {
        val NONE = JavaTypeQualifiers(null, null, false)
    }
}
