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

import java.util.Collection;

public class GroupingMessageCollector implements MessageCollector {

    private final MessageCollector delegate;

    // File path (nullable) -> message
    private final Multimap<String, Message> groupedMessages = LinkedHashMultimap.create();

    public GroupingMessageCollector(@NotNull MessageCollector delegate) {
        this.delegate = delegate;
    }

    @Override
    public void report(
            @NotNull CompilerMessageSeverity severity,
            @NotNull String message,
            @NotNull CompilerMessageLocation location
    ) {
        if (CompilerMessageSeverity.VERBOSE.contains(severity)) {
            delegate.report(severity, message, location);
        }
        else {
            groupedMessages.put(location.getPath(), new Message(severity, message, location));
        }
    }

    public void flush() {
        for (String path : groupedMessages.keySet()) {
            Collection<Message> messages = groupedMessages.get(path);
            for (Message message : messages) {
                delegate.report(message.severity, message.message, message.location);
            }
        }
        groupedMessages.clear();
    }

    private static class Message {
        private final CompilerMessageSeverity severity;
        private final String message;
        private final CompilerMessageLocation location;

        private Message(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location) {
            this.severity = severity;
            this.message = message;
            this.location = location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Message message1 = (Message) o;

            if (!location.equals(message1.location)) return false;
            if (!message.equals(message1.message)) return false;
            if (severity != message1.severity) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = severity.hashCode();
            result = 31 * result + message.hashCode();
            result = 31 * result + location.hashCode();
            return result;
        }
    }
}
