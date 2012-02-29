/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Severity;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author abreslav
 */
public interface MessageRenderer {

    MessageRenderer TAGS = new MessageRenderer() {
        private String renderWithStringSeverity(String severityString, String message, String path, int line, int column) {
            StringBuilder out = new StringBuilder();
            out.append("<").append(severityString);
            if (path != null) {
                out.append(" path=\"").append(path).append("\"");
                out.append(" line=\"").append(line).append("\"");
                out.append(" column=\"").append(column).append("\"");
            }
            out.append(">\n");

            out.append(message);

            out.append("</").append(severityString).append(">\n");
            return out.toString();
        }

        @Override
        public String render(@NotNull Severity severity, @NotNull String message, @Nullable String path, int line, int column) {
            return renderWithStringSeverity(severity.toString(), message, path, line, column);
        }

        @Override
        public String renderException(@NotNull Throwable e) {
            return renderWithStringSeverity("EXCEPTION", PLAIN.renderException(e), null, -1, -1);
        }
    };

    MessageRenderer PLAIN = new MessageRenderer() {
        @Override
        public String render(@NotNull Severity severity, @NotNull String message, @Nullable String path, int line, int column) {
            String position = path == null ? "" : path + ": (" + (line + ", " + column) + ") ";
            return severity + ": " + position + message;
        }

        @Override
        public String renderException(@NotNull Throwable e) {
            StringWriter out = new StringWriter();
            e.printStackTrace(new PrintWriter(out));
            return out.toString();
        }
    };

    String render(@NotNull Severity severity, @NotNull String message, @Nullable String path, int line, int column);
    String renderException(@NotNull Throwable e);
}
