/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

/** What a run of the test data manager did to a test data file. */
enum class FileActionKind {
    CREATED,
    UPDATED,
    DELETED,
    ;

    val existedBefore: Boolean get() = this != CREATED
    val existsAfter: Boolean get() = this != DELETED

    companion object {
        /** The net effect on a file that [existedBefore] and [existsAfter], or `null` if there is none. */
        fun of(existedBefore: Boolean, existsAfter: Boolean): FileActionKind? = when {
            !existedBefore && existsAfter -> CREATED
            existedBefore && !existsAfter -> DELETED
            existedBefore && existsAfter -> UPDATED
            else -> null
        }
    }
}
