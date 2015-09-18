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

import jline.console.ConsoleReader
import jline.console.history.FileHistory
import java.io.File

public class ConsoleReplCommandReader : ReplCommandReader {
    private val consoleReader = ConsoleReader("kotlin", System.`in`, System.`out`, null) apply {
        isHistoryEnabled = true
        expandEvents = false
        history = FileHistory(File(File(System.getProperty("user.home")), ".kotlin_history"))
    }

    public val fileHistory: FileHistory
        get() = consoleReader.history as FileHistory

    override fun readLine(prompt: String?) = consoleReader.readLine(prompt)
}