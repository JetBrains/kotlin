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

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class XmlMessageRenderer implements MessageRenderer {
    @Override
    public String renderPreamble() {
        return "<MESSAGES>";
    }

    @Override
    public String render(@NotNull CompilerMessageSeverity severity, @NotNull String message, @Nullable CompilerMessageLocation location) {
        StringBuilder out = new StringBuilder();
        String tagName = severity.getPresentableName();
        out.append("<").append(tagName);
        if (location != null) {
            out.append(" path=\"").append(e(location.getPath())).append("\"");
            out.append(" line=\"").append(location.getLine()).append("\"");
            out.append(" column=\"").append(location.getColumn()).append("\"");
        }
        out.append(">");

        out.append(e(message));

        out.append("</").append(tagName).append(">\n");
        return out.toString();
    }

    private static String e(String str) {
        return StringUtil.escapeXml(str);
    }

    @Override
    public String renderUsage(@NotNull String usage) {
        return render(CompilerMessageSeverity.STRONG_WARNING, usage, null);
    }

    @Override
    public String renderConclusion() {
        return "</MESSAGES>";
    }

    @Override
    public String getName() {
        return "XML";
    }
}
