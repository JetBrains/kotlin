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
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class FilteringMessageCollector implements MessageCollector {
    private final MessageCollector messageCollector;
    private final Predicate<CompilerMessageSeverity> decline;

    public FilteringMessageCollector(@NotNull MessageCollector messageCollector, @NotNull Predicate<CompilerMessageSeverity> decline) {
        this.messageCollector = messageCollector;
        this.decline = decline;
    }

    @Override
    public void clear() {
        messageCollector.clear();
    }

    @Override
    public void report(@NotNull CompilerMessageSeverity severity, @NotNull String message, @Nullable CompilerMessageLocation location) {
        if (!decline.test(severity)) {
            messageCollector.report(severity, message, location);
        }
    }

    @Override
    public boolean hasErrors() {
        return messageCollector.hasErrors();
    }
}
