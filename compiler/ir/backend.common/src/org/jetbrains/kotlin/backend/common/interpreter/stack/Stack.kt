/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

interface Stack {
    fun pushFrame(frame: Frame)
    fun popFrame(): Frame
    fun peekFrame(): Frame
}

interface Frame {
    fun addVar(state: State)
    fun getVar(name: String): State
}