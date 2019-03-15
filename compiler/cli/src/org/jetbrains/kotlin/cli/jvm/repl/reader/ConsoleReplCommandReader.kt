/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.repl.reader

import org.jetbrains.kotlin.cli.jvm.repl.ReplFromTerminal
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

class ConsoleReplCommandReader : ReplCommandReader {
    private val lineReader = LineReaderBuilder.builder()
        .appName("kotlin")
        .terminal(TerminalBuilder.terminal())
        .variable(LineReader.HISTORY_FILE, File(File(System.getProperty("user.home")), ".kotlinc_history").absolutePath)
        .build()
        .apply {
            setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION)
        }

    override fun readLine(next: ReplFromTerminal.WhatNextAfterOneLine): String? {
        val prompt = if (next == ReplFromTerminal.WhatNextAfterOneLine.INCOMPLETE) "... " else ">>> "
        return try {
            lineReader.readLine(prompt)
        } catch (e: UserInterruptException) {
            println("<interrupted>")
            System.out.flush()
            ""
        } catch (e: EndOfFileException) {
            null
        }
    }

    override fun flushHistory() = lineReader.history.save()

    private companion object {
        init {
            Logger.getLogger("org.jline").level = Level.OFF
        }
    }
}
