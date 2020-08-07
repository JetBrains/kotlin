/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.exceptions

/**
 * This class used to wrap 2 types of exceptions:
 * 1. exceptions from JVM such as: ArithmeticException, StackOverflowError and others
 * 2. exceptions defined by user
 */
class UserException(val exception: Throwable) : InterpreterException() {
    override fun fillInStackTrace(): Throwable = this
}

fun Throwable.throwAsUserException(): Nothing {
    throw UserException(this)
}