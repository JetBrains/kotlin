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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GroupingMessageCollector implements MessageCollector {
    private final MessageCollector delegate;

    // File path (nullable) -> message
    private final Multimap<String, Message> groupedMessages = LinkedHashMultimap.create();

    public GroupingMessageCollector(@NotNull MessageCollector delegate) {
        this.delegate = delegate;
    }

    @Override
    public void clear() {
        groupedMessages.clear();
    }

    @Override
    public void report(
            @NotNull CompilerMessageSeverity severity,
            @NotNull String message,
            @Nullable CompilerMessageLocation location
    ) {
        if (CompilerMessageSeverity.VERBOSE.contains(severity)) {
            delegate.report(severity, message, location);
        }
        else {
            groupedMessages.put(location != null ? location.getPath() : null, new Message(severity, message, location));
        }
    }

    @Override
    public boolean hasErrors() {
        return groupedMessages.entries().stream().anyMatch(entry -> entry.getValue().severity.isError());
    }

    public void flush() {
        boolean hasErrors = hasErrors();

        for (String path : sortedKeys()) {
            for (Message message : groupedMessages.get(path)) {
                if (!hasErrors || message.severity.isError() || message.severity == CompilerMessageSeverity.STRONG_WARNING) {
                    delegate.report(message.severity, message.message, message.location);
                }
            }
        }

        groupedMessages.clear();
    }

    @NotNull
    private Collection<String> sortedKeys() {
        // ensure that messages with no location i.e. perf, incomplete hierarchy are always reported first
        List<String> sortedKeys = new ArrayList<>(groupedMessages.keySet());
        sortedKeys.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
        return sortedKeys;
    }

    private static class Message {
        private final CompilerMessageSeverity severity;
        private final String message;
        private final CompilerMessageLocation location;

        private Message(@NotNull CompilerMessageSeverity severity, @NotNull String message, @Nullable CompilerMessageLocation location) {
            this.severity = severity;
            this.message = message;
            this.location = location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Message other = (Message) o;

            if (!Objects.equals(location, other.location)) return false;
            if (!message.equals(other.message)) return false;
            if (severity != other.severity) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = severity.hashCode();
            result = 31 * result + message.hashCode();
            result = 31 * result + (location != null ? location.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "[" + severity + "] " + message + (location != null ? " (at " + location + ")" : " (no location)");
        }
    }
}
