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

package org.jetbrains.kotlin.cli.jvm.repl.messages

import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorBasedReporter
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.ByteBuffer

class ReplTerminalDiagnosticMessageHolder : MessageCollectorBasedReporter, DiagnosticMessageHolder {
    private val outputStream = ByteArrayOutputStream()

    override val messageCollector: GroupingMessageCollector = GroupingMessageCollector(
            PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.WITHOUT_PATHS, false),
            false
    )

    override val renderedDiagnostics: String
        get() {
            messageCollector.flush()
            val bytes = outputStream.toByteArray()
            return Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString()
        }
}
