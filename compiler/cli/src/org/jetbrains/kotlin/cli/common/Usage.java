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
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KCallable;
import kotlin.reflect.KClass;
import kotlin.reflect.KProperty1;
import kotlin.reflect.jvm.ReflectJvmMapping;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.Argument;
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments;
import org.jetbrains.kotlin.cli.common.arguments.ParseCommandLineArgumentsKt;
import org.jetbrains.kotlin.cli.common.arguments.PreprocessCommandLineArgumentsKt;

import java.lang.reflect.Field;

public class Usage {
    public static final String BAT_DELIMITER_CHARACTERS_NOTE =
            "Note: on Windows, arguments that contain delimiter characters (whitespace, =, ;, ,) need to be surrounded with double quotes (\").";

    // The magic number 29 corresponds to the similar padding width in javac and scalac command line compilers
    private static final int OPTION_NAME_PADDING_WIDTH = 29;

    @NotNull
    public static <A extends CommonToolArguments> String render(@NotNull CLITool<A> tool, @NotNull A arguments) {
        boolean extraHelp = arguments.getExtraHelp();
        StringBuilder sb = new StringBuilder();
        appendln(sb, "Usage: " + tool.executableScriptFileName() + " <options> <source files>");
        appendln(sb, "where " + (extraHelp ? "advanced" : "possible") + " options include:");
        KClass<? extends CommonToolArguments> kClass = JvmClassMappingKt.getKotlinClass(arguments.getClass());
        for (KCallable<?> callable : kClass.getMembers()) {
            if (!(callable instanceof KProperty1)) continue;
            propertyUsage(sb, (KProperty1<?, ?>) callable, extraHelp);
        }

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

        if (!extraHelp && tool instanceof CLICompiler<?>) {
            appendln(sb, "");
            appendln(sb, "For details, see https://kotl.in/cli");
        }

        return sb.toString();
    }

    private static void propertyUsage(@NotNull StringBuilder sb, @NotNull KProperty1<?, ?> property, boolean extraHelp) {
        Field field = ReflectJvmMapping.getJavaField(property);
        Argument argument = field.getAnnotation(Argument.class);
        if (argument == null) return;

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
        appendln(sb, argument.description().replace("\n", "\n" + StringsKt.repeat(" ", OPTION_NAME_PADDING_WIDTH)));
    }

    private static void renderOptionJUsage(@NotNull StringBuilder sb) {
        int descriptionStart = sb.length() + OPTION_NAME_PADDING_WIDTH;
        sb.append("  -J<option>");
        while (sb.length() < descriptionStart) {
            sb.append(" ");
        }
        appendln(sb, "Pass an option directly to JVM");
    }

    private static void renderArgfileUsage(@NotNull StringBuilder sb) {
        int descriptionStart = sb.length() + OPTION_NAME_PADDING_WIDTH;
        sb.append("  ");
        sb.append(PreprocessCommandLineArgumentsKt.ARGFILE_ARGUMENT);
        sb.append("<argfile>");
        while (sb.length() < descriptionStart) {
            sb.append(" ");
        }
        appendln(sb, "Read compiler arguments and file paths from the given file");
    }

    private static void appendln(@NotNull StringBuilder sb, @NotNull String string) {
        sb.append(string);
        sb.append('\n');
    }
}
