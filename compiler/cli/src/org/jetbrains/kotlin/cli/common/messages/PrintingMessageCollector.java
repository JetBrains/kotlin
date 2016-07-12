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

package org.jetbrains.kotlin.cli.common.messages;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

public class PrintingMessageCollector implements MessageCollector {
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static final MessageCollector PLAIN_TEXT_TO_SYSTEM_ERR =
            new PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false);

    private final boolean verbose;
    private final PrintStream errStream;
    private final MessageRenderer messageRenderer;
    private boolean hasErrors = false;

    public PrintingMessageCollector(@NotNull PrintStream errStream, @NotNull MessageRenderer messageRenderer, boolean verbose) {
        this.verbose = verbose;
        this.errStream = errStream;
        this.messageRenderer = messageRenderer;
    }

    @Override
    public void report(
            @NotNull CompilerMessageSeverity severity,
            @NotNull String message,
            @NotNull CompilerMessageLocation location
    ) {
        if (!verbose && CompilerMessageSeverity.VERBOSE.contains(severity)) return;

        hasErrors |= severity.isError();

        errStream.println(messageRenderer.render(severity, message, location));
    }

    @Override
    public boolean hasErrors() {
        return hasErrors;
    }
}
