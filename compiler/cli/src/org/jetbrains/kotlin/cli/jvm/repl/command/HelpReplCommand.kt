/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.repl.command

import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommand
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommand.*
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommandContext

object HelpReplCommand : ReplCommand {
    override val name = "help"
    override val args = emptyList<String>()
    override val help = "display help message"

    override val constraints = ArgumentConstraints.Empty

    override fun run(context: ReplCommandContext, rawArgs: String): Boolean {
        val commands = ReplCommand.COMMANDS.map { (_, command) ->
            buildString {
                append(command.name)
                if (command.args.isNotEmpty()) {
                    append(' ')
                    command.args.joinTo(this) { "<$it>" }
                }
            } to command.help
        }

        val maxCommandLength = (commands.map { it.first.length }.max() ?: 0) + 4
        val renderedCommands = commands
            .joinToString("\n") { ":" + it.first + " ".repeat(maxCommandLength - it.first.length) + it.second }

        context.configuration.writer.printlnHelpMessage("Available commands:\n" + renderedCommands)
        return true
    }
}