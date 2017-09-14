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

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil

class MessageCollectorToOutputItemsCollectorAdapter(
        private val delegate: MessageCollector,
        private val outputCollector: OutputItemsCollector
) : MessageCollector by delegate {
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        // TODO: consider adding some other way of passing input -> output mapping from compiler, e.g. dedicated service
        OutputMessageUtil.parseOutputMessage(message)?.let {
            outputCollector.add(it.sourceFiles, it.outputFile)
        }
        delegate.report(severity, message, location)
    }
}