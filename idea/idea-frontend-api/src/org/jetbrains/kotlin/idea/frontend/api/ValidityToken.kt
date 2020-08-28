/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

abstract class ValidityToken {
    abstract fun isValid(): Boolean
    abstract fun getInvalidationReason(): String
}

@Suppress("NOTHING_TO_INLINE")
inline fun ValidityToken.assertIsValid() {
    if (!isValid()) {
        throw InvalidEntityAccessException("Access to invalid $this, invalidation reason is ${getInvalidationReason()}")
    }
}

class InvalidEntityAccessException(override val message: String): IllegalStateException()

