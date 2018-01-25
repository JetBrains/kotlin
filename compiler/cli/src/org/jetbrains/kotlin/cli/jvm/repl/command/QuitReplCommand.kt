/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.repl.command

import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommand
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommand.*
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommandContext

object QuitReplCommand : ReplCommand {
    override val name = "quit"
    override val args = emptyList<String>()
    override val help = "quit REPL"

    override val constraints = ArgumentConstraints.Empty

    override fun run(context: ReplCommandContext, rawArgs: String): Boolean {
        if (rawArgs.isNotEmpty()) {
            context.configuration.writer.outputCompileError("Expected zero arguments, got $rawArgs")
            return true
        }

        return false
    }
}