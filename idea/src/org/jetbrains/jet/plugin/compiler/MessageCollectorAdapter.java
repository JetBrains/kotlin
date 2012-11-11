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

package org.jetbrains.jet.plugin.compiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;

import static com.intellij.openapi.compiler.CompilerMessageCategory.*;

class MessageCollectorAdapter implements MessageCollector {
    private static final Logger LOG = Logger.getInstance(MessageCollectorAdapter.class);

    private final CompileContext compileContext;

    public MessageCollectorAdapter(CompileContext compileContext) {
        this.compileContext = compileContext;
    }

    @Override
    public void report(
            @NotNull CompilerMessageSeverity severity,
            @NotNull String message,
            @NotNull CompilerMessageLocation location
    ) {
        CompilerMessageCategory category = category(severity);
        compileContext.addMessage(category, message, "file://" + location.getPath(), location.getLine(), location.getColumn());
        if (severity == CompilerMessageSeverity.EXCEPTION) {
            LOG.error(message);
        }
        if (category == STATISTICS) {
            compileContext.getProgressIndicator().setText(message);
        }
    }

    private static CompilerMessageCategory category(CompilerMessageSeverity severity) {
        switch (severity) {
            case INFO:
                return INFORMATION;
            case ERROR:
                return ERROR;
            case WARNING:
                return WARNING;
            case EXCEPTION:
                return ERROR;
            case LOGGING:
                return STATISTICS;
        }
        throw new IllegalArgumentException("Unknown severity: " + severity);
    }
}
