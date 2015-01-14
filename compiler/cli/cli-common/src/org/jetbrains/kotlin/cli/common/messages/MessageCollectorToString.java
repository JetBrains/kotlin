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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class MessageCollectorToString implements MessageCollector {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final MessageCollector actualCollector = new MessageCollectorPlainTextToStream(new PrintStream(outputStream),
                                                                                           MessageCollectorPlainTextToStream.NON_VERBOSE);

    @Override
    public void report(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location) {
        actualCollector.report(severity, message, location);
    }

    private static final Charset UTF8 = Charset.forName("utf-8");

    @NotNull
    public String getString() {
        return UTF8.decode(ByteBuffer.wrap(outputStream.toByteArray())).toString();
    }
}
