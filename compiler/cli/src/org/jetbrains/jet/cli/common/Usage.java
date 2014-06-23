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

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

class Usage {
    private final PrintStream out;

    private Usage(@NotNull PrintStream out) {
        this.out = out;
    }

    public static void print(@NotNull PrintStream target, @NotNull CommonCompilerArguments arguments) {
        new Usage(target).print(arguments);
    }

    private void print(@NotNull CommonCompilerArguments arguments) {
        Class<?> clazz = arguments.getClass();
        out.println("Usage: " + clazz.getName());
        for (Class<?> currentClass = clazz; currentClass != null; currentClass = currentClass.getSuperclass()) {
            for (Field field : currentClass.getDeclaredFields()) {
                Argument argument = field.getAnnotation(Argument.class);
                if (argument == null) continue;

                try {
                    printFieldUsage(field, field.get(arguments), argument);
                }
                catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Could not use the field " + field + " as an argument field", e);
                }
            }
        }
    }

    private void printFieldUsage(@NotNull Field field, @Nullable Object defaultValue, @NotNull Argument argument) {
        String prefix = argument.prefix();
        Class<?> type = field.getType();
        String description = argument.description();

        StringBuilder sb = new StringBuilder("  ");
        sb.append(prefix);
        if (argument.value().equals("")) {
            sb.append(field.getName());
        }
        else {
            sb.append(argument.value());
        }
        if (!argument.alias().equals("")) {
            sb.append(" (");
            sb.append(prefix);
            sb.append(argument.alias());
            sb.append(")");
        }
        if (type == Boolean.TYPE || type == Boolean.class) {
            sb.append(" [flag] ");
            sb.append(description);
        }
        else {
            sb.append(" [");
            if (type.isArray()) {
                sb.append(type.getComponentType().getSimpleName());
                sb.append("[");
                sb.append(argument.delimiter());
                sb.append("]");
            }
            else {
                sb.append(type.getSimpleName());
            }
            sb.append("] ");
            sb.append(description);
            if (defaultValue != null) {
                sb.append(" (");
                if (type.isArray()) {
                    int len = Array.getLength(defaultValue);
                    List<Object> list = new ArrayList<Object>(len);
                    for (int i = 0; i < len; i++) {
                        list.add(Array.get(defaultValue, i));
                    }
                    sb.append(list);
                }
                else {
                    sb.append(defaultValue);
                }
                sb.append(")");
            }
        }
        out.println(sb);
    }
}
