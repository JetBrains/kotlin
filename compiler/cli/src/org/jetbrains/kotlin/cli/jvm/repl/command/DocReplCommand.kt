/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.repl.command

import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommand
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommand.*
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommandContext

object DocReplCommand : ReplCommand {
    override val name = "doc"
    override val args = listOf("symbol")
    override val help = "display documentation for the given symbol"

    override val constraints = ArgumentConstraints.Raw

    override fun run(context: ReplCommandContext, rawArgs: String): Boolean {
        val docComment = context.interpreter.doc(rawArgs)
        context.configuration.writer.printlnHelpMessage(docComment ?: "No documentation found.")
        return true
    }
}