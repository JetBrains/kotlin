/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.cli.common.messages;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.Collection;

public class PrintingMessageCollector implements MessageCollector {

    private final boolean verbose;
    private final PrintStream errStream;
    private final MessageRenderer messageRenderer;

    // File path (nullable) -> error message
    private final Multimap<String, String> groupedMessages = LinkedHashMultimap.create();

    public PrintingMessageCollector(
            @NotNull PrintStream errStream,
            @NotNull MessageRenderer messageRenderer,
            boolean verbose
    ) {
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

        String text = messageRenderer.render(severity, message, location);
        if (severity == CompilerMessageSeverity.LOGGING || severity == CompilerMessageSeverity.OUTPUT) {
            if (!verbose) {
                return;
            }
            errStream.println(text);
        }
        else {
            groupedMessages.put(location.getPath(), text);
        }
    }

    public void printToErrStream() {
        if (!groupedMessages.isEmpty()) {
            for (String path : groupedMessages.keySet()) {
                Collection<String> messageTexts = groupedMessages.get(path);
                for (String text : messageTexts) {
                    errStream.println(text);
                }
            }
        }
    }
}
