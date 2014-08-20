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

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.EnumSet;
import java.util.Set;

public class MessageCollectorPlainTextToStream implements MessageCollector {
    public static final EnumSet<CompilerMessageSeverity> NON_VERBOSE = EnumSet.complementOf(CompilerMessageSeverity.VERBOSE);

    public static final MessageCollector PLAIN_TEXT_TO_SYSTEM_ERR = new MessageCollectorPlainTextToStream(System.err, NON_VERBOSE);

    @NotNull
    private final PrintStream stream;
    @NotNull
    private final Set<CompilerMessageSeverity> severitiesToPrint;

    public MessageCollectorPlainTextToStream(@NotNull PrintStream stream, @NotNull Set<CompilerMessageSeverity> severities) {
        this.stream = stream;
        this.severitiesToPrint = severities;
    }

    @Override
    public void report(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location) {
        if (severitiesToPrint.contains(severity)) {
            stream.println(MessageRenderer.PLAIN.render(severity, message, location));
        }
    }

}
