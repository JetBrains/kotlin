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

package org.jetbrains.kotlin.cli.jvm.repl.configuration

import org.jetbrains.kotlin.cli.jvm.repl.ReplExceptionReporter
import org.jetbrains.kotlin.cli.jvm.repl.messages.ConsoleDiagnosticMessageHolder
import org.jetbrains.kotlin.cli.jvm.repl.reader.ConsoleReplCommandReader
import org.jetbrains.kotlin.cli.jvm.repl.writer.ConsoleReplWriter

class ConsoleReplConfiguration : ReplConfiguration {
    override val writer = ConsoleReplWriter()

    override val exceptionReporter
        get() = ReplExceptionReporter

    override val commandReader = ConsoleReplCommandReader()

    override val allowIncompleteLines: Boolean
        get() = true

    override val executionInterceptor
        get() = SnippetExecutionInterceptor

    override fun createDiagnosticHolder() = ConsoleDiagnosticMessageHolder()
}
