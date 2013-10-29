/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package com.sampullara.cli;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ComparatorUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ArgumentUtils {

    private ArgumentUtils() {}

    @NotNull
    public static <T> List<String> convertArgumentsToStringList(@NotNull T arguments, @NotNull T defaultArguments) {
        List<String> result = new ArrayList<String>();
        convertArgumentsToStringList(arguments, defaultArguments, result);
        return result;
    }

    public static <T> void convertArgumentsToStringList(@NotNull T arguments, @NotNull T defaultArguments, @NotNull List<String> result) {
        convertArgumentsToStringList(arguments, defaultArguments, arguments.getClass(), result);
    }

    private static <T> void convertArgumentsToStringList(T arguments, T defaultArguments, Class clazz, List<String> result) {
        Class superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            convertArgumentsToStringList(arguments, defaultArguments, superClazz, result);
        }

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

            if (ComparatorUtil.equalsNullable(value, defaultValue)) continue;

            String name = Args.getAlias(argument);
            if (name == null) {
                name = Args.getName(argument, field);
            }

            Class<?> fieldType = field.getType();

            if (fieldType.isArray()) {
                Object[] values = (Object[]) value;
                if (values.length == 0) continue;
                value = StringUtil.join(values, Function.TO_STRING, argument.delimiter());
            }

            result.add(argument.prefix() + name);

            if (fieldType == boolean.class || fieldType == Boolean.class) continue;

            result.add(value.toString());
        }
    }

}
