/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

class ExceptionFromTestError(cause: Throwable) : AssertionError(cause) {
    override val message: String
        get() = "Exception was thrown"

    override val cause: Throwable
        get() = super.cause!!
}
