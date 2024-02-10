/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import kotlin.text.StringsKt;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.internal.CLibrary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties;
import org.jetbrains.kotlin.cli.common.PropertiesKt;
import org.jetbrains.kotlin.util.capitalizeDecapitalize.CapitalizeDecapitalizeKt;

import java.util.EnumSet;
import java.util.Set;

import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*;

public abstract class PlainTextMessageRenderer implements MessageRenderer {
    private static final boolean IS_STDERR_A_TTY;

    static {
        boolean isStderrATty = false;
        // TODO: investigate why ANSI escape codes on Windows only work in REPL for some reason
        if (!PropertiesKt.isWindows() && "true".equals(CompilerSystemProperties.KOTLIN_COLORS_ENABLED_PROPERTY.getValue())) {
            try {
                isStderrATty = CLibrary.isatty(CLibrary.STDERR_FILENO) != 0;
            }
            catch (UnsatisfiedLinkError ignored) {
            }
        }
        IS_STDERR_A_TTY = isStderrATty;
    }

    private static final String LINE_SEPARATOR = System.lineSeparator();

    private static final Set<CompilerMessageSeverity> IMPORTANT_MESSAGE_SEVERITIES = EnumSet.of(EXCEPTION, ERROR, STRONG_WARNING, WARNING);

    private final boolean colorEnabled;

    public PlainTextMessageRenderer() {
        this(IS_STDERR_A_TTY);
    }

    // This constructor can be used in a compilation server to still be able to generate colored output, even if stderr is not a TTY.
    @SuppressWarnings("WeakerAccess")
    public PlainTextMessageRenderer(boolean colorEnabled) {
        this.colorEnabled = colorEnabled;
    }

    @Override
    public String renderPreamble() {
        return "";
    }

    @Override
    public String render(@NotNull CompilerMessageSeverity severity, @NotNull String message, @Nullable CompilerMessageSourceLocation location) {
        StringBuilder result = new StringBuilder();

        int line = location != null ? location.getLine() : -1;
        int column = location != null ? location.getColumn() : -1;
        int lineEnd = location != null ? location.getLineEnd() : -1;
        int columnEnd = location != null ? location.getColumnEnd() : -1;
        String lineContent = location != null ? location.getLineContent() : null;

        String path = location != null ? getPath(location) : null;
        if (path != null) {
            result.append(path);
            result.append(":");
            if (line > 0) {
                result.append(line).append(":");
                if (column > 0) {
                    result.append(column).append(":");
                }
            }
            result.append(" ");
        }

        if (this.colorEnabled) {
            Ansi ansi = Ansi.ansi()
                    .bold()
                    .fg(severityColor(severity))
                    .a(severity.getPresentableName())
                    .a(": ")
                    .reset();

            if (IMPORTANT_MESSAGE_SEVERITIES.contains(severity)) {
                ansi.bold();
            }

            // Only make the first line of the message bold. Otherwise long overload ambiguity errors or exceptions are hard to read
            String decapitalized = decapitalizeIfNeeded(message);
            int firstNewline = decapitalized.indexOf(LINE_SEPARATOR);
            if (firstNewline < 0) {
                result.append(ansi.a(decapitalized).reset());
            }
            else {
                result.append(ansi.a(decapitalized.substring(0, firstNewline)).reset().a(decapitalized.substring(firstNewline)));
            }
        }
        else {
            result.append(severity.getPresentableName());
            result.append(": ");
            result.append(decapitalizeIfNeeded(message));
        }

        if (lineContent != null && 1 <= column && column <= lineContent.length() + 1) {
            result.append(LINE_SEPARATOR);
            result.append(lineContent);
            result.append(LINE_SEPARATOR);
            result.append(StringsKt.repeat(" ", column - 1));
            if (lineEnd > line) {
                result.append(StringsKt.repeat("^", lineContent.length() - column + 1));
            } else if (lineEnd == line && columnEnd > column) {
                result.append(StringsKt.repeat("^", columnEnd - column));
            } else {
                result.append("^");
            }
        }

        return result.toString();
    }

    @NotNull
    private static String decapitalizeIfNeeded(@NotNull String message) {
        // TODO: invent something more clever
        // An ad-hoc heuristic to prevent decapitalization of some names
        if (message.startsWith("Java") || message.startsWith("Kotlin")) {
            return message;
        }

        // For abbreviations and capitalized text
        if (message.length() >= 2 && Character.isUpperCase(message.charAt(0)) && Character.isUpperCase(message.charAt(1))) {
            return message;
        }

        return CapitalizeDecapitalizeKt.decapitalizeAsciiOnly(message);
    }

    @NotNull
    private static Ansi.Color severityColor(@NotNull CompilerMessageSeverity severity) {
        switch (severity) {
            case EXCEPTION:
                return Ansi.Color.RED;
            case ERROR:
                return Ansi.Color.RED;
            case STRONG_WARNING:
                return Ansi.Color.YELLOW;
            case WARNING:
                return Ansi.Color.YELLOW;
            case INFO:
                return Ansi.Color.BLUE;
            case LOGGING:
                return Ansi.Color.BLUE;
            case OUTPUT:
                return Ansi.Color.BLUE;
            default:
                throw new UnsupportedOperationException("Unknown severity: " + severity);
        }
    }

    @Nullable
    protected abstract String getPath(@NotNull CompilerMessageSourceLocation location);

    @Override
    public String renderUsage(@NotNull String usage) {
        return usage;
    }

    @Override
    public String renderConclusion() {
        return "";
    }

    public void enableColorsIfNeeded() {
        if (colorEnabled) {
            AnsiConsole.systemInstall();
        }
    }

    public void disableColorsIfNeeded() {
        if (colorEnabled) {
            AnsiConsole.systemUninstall();
        }
    }
}
