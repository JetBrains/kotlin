/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

open class ScratchOutputHandlerAdapter: ScratchOutputHandler {
    override fun onStart(file: ScratchFile) {}
    override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {}
    override fun error(file: ScratchFile, message: String) {}
    override fun onFinish(file: ScratchFile) {}
    override fun clear(file: ScratchFile) {}
}