/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.repl.command

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommand
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommand.*
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommandContext
import java.io.File
import java.io.IOException

object LoadReplCommand : ReplCommand {
    override val name = "load"
    override val args = listOf("file")
    override val help = "evaluate a script file"

    override val constraints = ArgumentConstraints.Raw

    override fun run(context: ReplCommandContext, rawArgs: String): Boolean {
        fun printError(text: String) = context.configuration.writer.outputCompileError(text)

        val file = File(rawArgs).takeIf { it.isFile } ?: run {
            printError("File not exists: ${File(rawArgs).absolutePath}")
            return true
        }

        try {
            val scriptText = FileUtil.loadFile(file)
            context.interpreter.eval(scriptText)
        } catch (e: IOException) {
            printError("Can not load script: ${e.message}")
        }

        return true
    }
}