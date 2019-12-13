/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.output

import org.jetbrains.kotlin.idea.scratch.ScratchExpression
import org.jetbrains.kotlin.idea.scratch.ScratchFile

interface ScratchOutputHandler {
    fun onStart(file: ScratchFile)
    fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput)
    fun error(file: ScratchFile, message: String)
    fun onFinish(file: ScratchFile)
    fun clear(file: ScratchFile)
}

data class ScratchOutput(val text: String, val type: ScratchOutputType)

enum class ScratchOutputType {
    RESULT,
    OUTPUT,
    ERROR
}

open class ScratchOutputHandlerAdapter : ScratchOutputHandler {
    override fun onStart(file: ScratchFile) {}
    override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {}
    override fun error(file: ScratchFile, message: String) {}
    override fun onFinish(file: ScratchFile) {}
    override fun clear(file: ScratchFile) {}
}