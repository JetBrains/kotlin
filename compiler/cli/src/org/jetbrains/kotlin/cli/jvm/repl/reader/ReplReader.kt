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

import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.jvm.repl.KotlinRepl
import org.jetbrains.kotlin.cli.jvm.repl.messages.unescapeLineBreaks

interface ReplReader {
    fun readLine(next: KotlinRepl.WhatNextAfterOneLine): String?
    fun flushHistory()

    tailrec fun runLoop(processor: ReplProcessor) {
        val nextLine = KotlinRepl.WhatNextAfterOneLine.READ_LINE
        if (processLine(processor, nextLine) != KotlinRepl.WhatNextAfterOneLine.QUIT) {
            runLoop(processor)
        }
    }

    private fun processLine(
        processor: ReplProcessor,
        next: KotlinRepl.WhatNextAfterOneLine
    ): KotlinRepl.WhatNextAfterOneLine {
        val rawLine = readLine(next) ?: return KotlinRepl.WhatNextAfterOneLine.QUIT
        val line = unescapeLineBreaks(rawLine)

        if (isCommand(line)) {
            val commandName = line.substringBefore(' ')
            val rawArgs = line.substringAfter(' ', missingDelimiterValue = "").trim()

            return if (processor.processCommand(commandName.drop(1), rawArgs)) {
                KotlinRepl.WhatNextAfterOneLine.READ_LINE
            } else {
                KotlinRepl.WhatNextAfterOneLine.QUIT
            }
        }

        val lineResult = processor.evaluate(line)
        return if (lineResult is ReplEvalResult.Incomplete) {
            KotlinRepl.WhatNextAfterOneLine.INCOMPLETE
        } else {
            KotlinRepl.WhatNextAfterOneLine.READ_LINE
        }
    }

    private fun isCommand(line: String) = line.startsWith(":") && (line.length == 1 || line[1] != ':')
}