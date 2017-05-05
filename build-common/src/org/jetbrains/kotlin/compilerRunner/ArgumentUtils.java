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

package org.jetbrains.kotlin.compilerRunner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.Argument;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.ParseCommandLineArgumentsKt;
import org.jetbrains.kotlin.utils.StringsKt;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ArgumentUtils {
    private ArgumentUtils() {}

    @NotNull
    public static List<String> convertArgumentsToStringList(@NotNull CommonCompilerArguments arguments)
            throws InstantiationException, IllegalAccessException {
        List<String> result = new ArrayList<>();
        convertArgumentsToStringList(arguments, arguments.getClass().newInstance(), arguments.getClass(), result);
        result.addAll(arguments.freeArgs);
        return result;
    }

    private static void convertArgumentsToStringList(
            @NotNull CommonCompilerArguments arguments,
            @NotNull CommonCompilerArguments defaultArguments,
            @NotNull Class<?> clazz,
            @NotNull List<String> result
    ) throws IllegalAccessException, InstantiationException {
        for (Field field : clazz.getDeclaredFields()) {
            Argument argument = field.getAnnotation(Argument.class);
            if (argument == null) continue;

            Object value;
            Object defaultValue;
            try {
                value = field.get(arguments);
                defaultValue = field.get(defaultArguments);
            }
            catch (IllegalAccessException ignored) {
                // skip this field
                continue;
            }

            if (Objects.equals(value, defaultValue)) continue;

            Class<?> fieldType = field.getType();

            if (fieldType.isArray()) {
                Object[] values = (Object[]) value;
                if (values.length == 0) continue;
                value = StringsKt.join(Arrays.asList(values), ",");
            }

            result.add(argument.value());

            if (fieldType == boolean.class || fieldType == Boolean.class) continue;

            if (ParseCommandLineArgumentsKt.isAdvanced(argument)) {
                result.set(result.size() - 1, argument.value() + "=" + value.toString());
            }
            else {
                result.add(value.toString());
            }
        }

        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            convertArgumentsToStringList(arguments, defaultArguments, superClazz, result);
        }
    }
}
