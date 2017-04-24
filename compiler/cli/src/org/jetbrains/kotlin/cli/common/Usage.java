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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.arguments.Argument;
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments;
import org.jetbrains.kotlin.cli.common.arguments.ParseCommandLineArgumentsKt;

import java.io.PrintStream;
import java.lang.reflect.Field;

class Usage {
    // The magic number 29 corresponds to the similar padding width in javac and scalac command line compilers
    private static final int OPTION_NAME_PADDING_WIDTH = 29;

    public static <A extends CommonToolArguments> void print(
            @NotNull PrintStream target, @NotNull CLITool<A> compiler, @NotNull A arguments
    ) {
        target.println("Usage: " + compiler.executableScriptFileName() + " <options> <source files>");
        target.println("where " + (arguments.extraHelp ? "advanced" : "possible") + " options include:");
        for (Class<?> clazz = arguments.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                String usage = fieldUsage(field, arguments.extraHelp);
                if (usage != null) {
                    target.println(usage);
                }
            }
        }

        if (arguments.extraHelp) {
            target.println();
            target.println("Advanced options are non-standard and may be changed or removed without any notice.");
        }
    }

    @Nullable
    private static String fieldUsage(@NotNull Field field, boolean extraHelp) {
        Argument argument = field.getAnnotation(Argument.class);
        if (argument == null) return null;

        if (extraHelp != ParseCommandLineArgumentsKt.isAdvanced(argument)) return null;

        StringBuilder sb = new StringBuilder("  ");
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

        int width = OPTION_NAME_PADDING_WIDTH - 1;
        if (sb.length() >= width + 5) { // Break the line if it's too long
            sb.append("\n");
            width += sb.length();
        }
        while (sb.length() < width) {
            sb.append(" ");
        }

        sb.append(" ");
        sb.append(argument.description());
        return sb.toString();
    }
}
