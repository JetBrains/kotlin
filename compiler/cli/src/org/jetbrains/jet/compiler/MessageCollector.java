/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.compiler;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Severity;

import java.io.PrintStream;
import java.util.Collection;

/**
* @author alex.tkachman
*/
/*package*/ class MessageCollector {
    // File path (nullable) -> error message
    private final Multimap<String, String> groupedMessages = LinkedHashMultimap.create();

    private final MessageRenderer renderer;
    private boolean hasErrors;

    public MessageCollector(@NotNull MessageRenderer renderer) {
        this.renderer = renderer;
    }

    public void report(@NotNull Severity severity, @NotNull String message, @Nullable String path, int line, int column) {
        hasErrors |= severity == Severity.ERROR;
        groupedMessages.put(path, renderer.render(severity, message, path, line, column));
    }

    public void printTo(@NotNull PrintStream out) {
        if (!groupedMessages.isEmpty()) {
            for (String path : groupedMessages.keySet()) {
                Collection<String> diagnostics = groupedMessages.get(path);
                for (String diagnostic : diagnostics) {
                    out.println(diagnostic);
                }
            }
        }
    }

    public boolean hasErrors() {
        return hasErrors;
    }
}
