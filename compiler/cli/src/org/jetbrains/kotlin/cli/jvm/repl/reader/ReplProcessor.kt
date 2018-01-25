/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.repl.reader

import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.jvm.repl.ReplInterpreter
import org.jetbrains.kotlin.cli.jvm.repl.command.*
import org.jetbrains.kotlin.cli.jvm.repl.configuration.ReplConfiguration

class ReplProcessor(
    interpreter: ReplInterpreter,
    configuration: ReplConfiguration
) {
    private val context = ReplCommandContext(interpreter, configuration)

    fun processCommand(commandName: String, rawArgs: String): Boolean {
        val command = ReplCommand.COMMANDS[commandName] ?: run {
            context.configuration.writer.printlnHelpMessage("Unknown command\n" + "Type :help for help")
            return true
        }

        return command.run(context, rawArgs)
    }

    fun evaluate(text: String): ReplEvalResult {
        val evalResult = context.interpreter.eval(text)
        val writer = context.configuration.writer

        when (evalResult) {
            is ReplEvalResult.ValueResult, is ReplEvalResult.UnitResult -> {
                writer.notifyCommandSuccess()
                if (evalResult is ReplEvalResult.ValueResult) {
                    writer.outputCommandResult(evalResult.value.toString())
                }
            }
            is ReplEvalResult.Error.Runtime -> writer.outputRuntimeError(evalResult.message)
            is ReplEvalResult.Error.CompileTime -> writer.outputRuntimeError(evalResult.message)
            is ReplEvalResult.Incomplete -> writer.notifyIncomplete()
        }
        return evalResult
    }

    fun complete(line: String, position: Int): List<String> {
        return context.interpreter.complete(line.substring(0, position)).map { it.name.asString() }
    }
}

interface ReplCommand {
    val name: String
    val args: List<String>
    val help: String

    val constraints: ArgumentConstraints

    /** Returns `true` if REPL should continue processing the next line. */
    fun run(context: ReplCommandContext, rawArgs: String): Boolean

    sealed class ArgumentConstraints {
        object Empty : ArgumentConstraints()
        object Raw : ArgumentConstraints()
    }

    companion object {
        val COMMANDS: Map<String, ReplCommand> = listOf(
            ClassesReplCommand,
            DocReplCommand,
            LoadReplCommand,
            HelpReplCommand,
            QuitReplCommand
        ).map { it.name to it }.toMap()
    }
}

class ReplCommandContext(val interpreter: ReplInterpreter, val configuration: ReplConfiguration)