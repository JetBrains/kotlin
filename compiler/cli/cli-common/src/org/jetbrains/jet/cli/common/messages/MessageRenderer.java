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

package org.jetbrains.jet.cli.common.messages;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;

public interface MessageRenderer {

    MessageRenderer TAGS = new MessageRenderer() {
        @Override
        public String renderPreamble() {
            return "<MESSAGES>";
        }

        @Override
        public String render(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location) {
            StringBuilder out = new StringBuilder();
            out.append("<").append(severity.toString());
            if (location.getPath() != null) {
                out.append(" path=\"").append(e(location.getPath())).append("\"");
                out.append(" line=\"").append(location.getLine()).append("\"");
                out.append(" column=\"").append(location.getColumn()).append("\"");
            }
            out.append(">");

            out.append(e(message));

            out.append("</").append(severity.toString()).append(">\n");
            return out.toString();
        }

        private String e(String str) {
            return StringUtil.escapeXml(str);
        }

        @Override
        public String renderException(@NotNull Throwable e) {
            return render(CompilerMessageSeverity.EXCEPTION, PLAIN.renderException(e), CompilerMessageLocation.NO_LOCATION);
        }

        @Override
        public String renderConclusion() {
            return "</MESSAGES>";
        }
    };

    MessageRenderer PLAIN = new MessageRenderer() {
        @Override
        public String renderPreamble() {
            return "";
        }

        @Override
        public String render(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location) {
            String path = location.getPath();
            String position = path == null ? "" : path + ": (" + (location.getLine() + ", " + location.getColumn()) + ") ";
            return severity + ": " + position + message;
        }

        @Override
        public String renderException(@NotNull Throwable e) {
            StringWriter out = new StringWriter();
            //noinspection IOResourceOpenedButNotSafelyClosed
            e.printStackTrace(new PrintWriter(out));
            return out.toString();
        }

        @Override
        public String renderConclusion() {
            return "";
        }
    };

    String renderPreamble();
    String render(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location);
    String renderException(@NotNull Throwable e);
    String renderConclusion();
}
