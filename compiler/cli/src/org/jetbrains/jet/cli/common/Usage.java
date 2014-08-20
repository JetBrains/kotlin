/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.cli.common;

import com.sampullara.cli.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.ValueDescription;

import java.io.PrintStream;
import java.lang.reflect.Field;

class Usage {
    // The magic number 29 corresponds to the similar padding width in javac and scalac command line compilers
    private static final int OPTION_NAME_PADDING_WIDTH = 29;

    public static void print(@NotNull PrintStream target, @NotNull CommonCompilerArguments arguments, boolean extraHelp) {
        target.println("Usage: " + arguments.executableScriptFileName() + " <options> <source files>");
        target.println("where " + (extraHelp ? "advanced" : "possible") + " options include:");
        for (Class<?> clazz = arguments.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                String usage = fieldUsage(field, extraHelp);
                if (usage != null) {
                    target.println(usage);
                }
            }
        }

        if (extraHelp) {
            target.println();
            target.println("Advanced options are non-standard and may be changed or removed without any notice.");
        }
    }

    @Nullable
    private static String fieldUsage(@NotNull Field field, boolean extraHelp) {
        Argument argument = field.getAnnotation(Argument.class);
        if (argument == null) return null;
        ValueDescription description = field.getAnnotation(ValueDescription.class);

        String value = argument.value();
        boolean extraOption = value.startsWith("X") && value.length() > 1;
        if (extraHelp != extraOption) return null;

        String prefix = argument.prefix();

        StringBuilder sb = new StringBuilder("  ");
        sb.append(prefix);
        sb.append(value);
        if (!argument.alias().isEmpty()) {
            sb.append(" (");
            sb.append(prefix);
            sb.append(argument.alias());
            sb.append(")");
        }
        if (description != null) {
            sb.append(" ");
            sb.append(description.value());
        }
        while (sb.length() < OPTION_NAME_PADDING_WIDTH) {
            sb.append(" ");
        }
        sb.append(argument.description());
        return sb.toString();
    }
}
