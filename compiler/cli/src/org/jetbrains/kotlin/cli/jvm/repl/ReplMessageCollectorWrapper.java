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

package org.jetbrains.kotlin.cli.jvm.repl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ReplMessageCollectorWrapper {
    private static final Charset UTF8 = Charset.forName("utf-8");

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final MessageCollector actualCollector =
            new PrintingMessageCollector(new PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, false);

    @NotNull
    public MessageCollector getMessageCollector() {
        return actualCollector;
    }

    @NotNull
    public String getString() {
        return UTF8.decode(ByteBuffer.wrap(outputStream.toByteArray())).toString();
    }
}
