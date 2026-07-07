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

package org.jetbrains.kotlin.cli.common;

import com.intellij.openapi.util.SystemInfo;
import kotlin.KotlinVersion;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.*;

import java.util.stream.Stream;

import static org.jetbrains.kotlin.cli.common.UtilsKt.parseKotlinVersion;
import static org.jetbrains.kotlin.cli.common.arguments.ParseCommandLineArgumentsKt.getArgumentsInfo;

public class Usage {
    public static final String BAT_DELIMITER_CHARACTERS_NOTE =
            "Note: on Windows, arguments that contain delimiter characters (whitespace, =, ;, ,) need to be surrounded with double quotes (\").";

    // The magic number 29 corresponds to the similar padding width in javac and scalac command line compilers
    private static final int OPTION_NAME_PADDING_WIDTH = 29;
    private static final String PADDING_STRING = StringsKt.repeat(" ", OPTION_NAME_PADDING_WIDTH);

    @NotNull
    public static <A extends CommonCompilerArguments> String render(@NotNull CLICompiler<A> compiler, @NotNull A arguments) {
        boolean extraHelp = arguments.getExtraHelp();
        StringBuilder sb = new StringBuilder();
        appendln(sb, "Usage: " + compiler.executableScriptFileName() + " <options> <source files>");
        appendln(sb, "where " + (extraHelp ? "advanced" : "possible") + " options include:");

        // Use distinct because same arguments can be bound to different names (shortName, deprecatedName)
        Stream<ArgumentField> argumentFields = getArgumentsInfo(arguments.getClass()).getCliArgNameToArguments().values().stream().distinct();
        argumentFields.forEach(argumentField -> fieldUsage(sb, argumentField, extraHelp));

        if (extraHelp) {
            appendln(sb, "");
            appendln(sb, "Advanced options are non-standard and may be changed or removed without any notice.");
        }
        else {
            renderOptionJUsage(sb);
            renderArgfileUsage(sb);
        }

        if (SystemInfo.isWindows) {
            appendln(sb, "");
            appendln(sb, BAT_DELIMITER_CHARACTERS_NOTE);
        }

        if (!extraHelp) {
            appendln(sb, "");
            appendln(sb, "For details, see https://kotl.in/cli");
        }

        return sb.toString();
    }

    private static void fieldUsage(@NotNull StringBuilder sb, @NotNull ArgumentField argumentField, boolean extraHelp) {
        Argument argument = argumentField.getArgument();

        if (argument.isObsolete()) return;
        if (ParseCommandLineArgumentsKt.isInternal(argument)) return;
        if (extraHelp != ParseCommandLineArgumentsKt.isAdvanced(argument)) return;

        int startLength = sb.length();
        sb.append("  ");
        sb.append(argument.value());

        if (!argument.shortName().isEmpty()) {
            sb.append(" (");
            sb.append(argument.shortName());
            sb.append(")");
        }

        if (!argument.valueDescription().isEmpty()) {
            sb.append(ParseCommandLineArgumentsKt.isAdvanced(argument) ? "=" : " ");
            sb.append(argument.valueDescription());
        }

        int margin = startLength + OPTION_NAME_PADDING_WIDTH - 1;
        if (sb.length() >= margin + 5) { // Break the line if it's too long
            sb.append("\n");
            margin += sb.length() - startLength;
        }
        while (sb.length() < margin) {
            sb.append(" ");
        }

        sb.append(" ");

        appendln(sb, argument.description().replace("\n", "\n" + PADDING_STRING));

        kotlin.Deprecated deprecatedAnnotation = argumentField.getDeprecatedAnnotation();
        if (deprecatedAnnotation != null) {
            sb.append(PADDING_STRING);
            sb.append("The option is ");
            // The value is generated automatically based on KotlinReleaseVersion entries, thus it's expected to be always valid.
            KotlinVersion argDeprecatedVersion = parseKotlinVersion(argument.deprecatedVersion());
            boolean isAlreadyDeprecated = argDeprecatedVersion.compareTo(KotlinVersion.CURRENT) <= 0;
            sb.append(isAlreadyDeprecated ? "deprecated since " : "will be deprecated in ");
            sb.append("Kotlin ").append(argDeprecatedVersion).append('.');
            if (isAlreadyDeprecated) {
                sb.append(" It will be removed in one of the future releases.");
            }
            String message = deprecatedAnnotation.message();
            if (!message.isEmpty()) {
                sb.append(' ').append(message.replace("\n", "\n" + PADDING_STRING));
            }
            sb.append('\n');
        }
    }

    private static void renderOptionJUsage(@NotNull StringBuilder sb) {
        int descriptionStart = sb.length() + OPTION_NAME_PADDING_WIDTH;
        sb.append("  -J<option>");
        while (sb.length() < descriptionStart) {
            sb.append(" ");
        }
        appendln(sb, "Pass an option directly to JVM.");
    }

    private static void renderArgfileUsage(@NotNull StringBuilder sb) {
        int descriptionStart = sb.length() + OPTION_NAME_PADDING_WIDTH;
        sb.append("  ");
        sb.append(PreprocessCommandLineArgumentsKt.ARGFILE_ARGUMENT);
        sb.append("<argfile>");
        while (sb.length() < descriptionStart) {
            sb.append(" ");
        }
        appendln(sb, "Read compiler arguments and file paths from the given file.");
    }

    private static void appendln(@NotNull StringBuilder sb, @NotNull String string) {
        sb.append(string);
        sb.append('\n');
    }
}
