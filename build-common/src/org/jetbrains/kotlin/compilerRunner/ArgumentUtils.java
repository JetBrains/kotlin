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

import kotlin.collections.CollectionsKt;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KProperty1;
import kotlin.reflect.KVisibility;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.arguments.Argument;
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments;
import org.jetbrains.kotlin.cli.common.arguments.InternalArgument;
import org.jetbrains.kotlin.cli.common.arguments.ParseCommandLineArgumentsKt;
import org.jetbrains.kotlin.utils.StringsKt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ArgumentUtils {
    private ArgumentUtils() {}

    @NotNull
    public static List<String> convertArgumentsToStringList(@NotNull CommonToolArguments arguments)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        List<String> result = new ArrayList<>();
        Class<? extends CommonToolArguments> argumentsClass = arguments.getClass();
        convertArgumentsToStringList(arguments, argumentsClass.newInstance(), JvmClassMappingKt.getKotlinClass(argumentsClass), result);
        result.addAll(arguments.getFreeArgs());
        result.addAll(CollectionsKt.map(arguments.getInternalArguments(), InternalArgument::getStringRepresentation));
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void convertArgumentsToStringList(
            @NotNull CommonToolArguments arguments,
            @NotNull CommonToolArguments defaultArguments,
            @NotNull KClass<?> clazz,
            @NotNull List<String> result
    ) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        for (KProperty1 property : KClasses.getMemberProperties(clazz)) {
            Argument argument = findInstance(property.getAnnotations(), Argument.class);
            if (argument == null) continue;

            if (property.getVisibility() != KVisibility.PUBLIC) continue;

            Object value = property.get(arguments);
            Object defaultValue = property.get(defaultArguments);

            if (value == null || Objects.equals(value, defaultValue)) continue;

            Type propertyJavaType = ReflectJvmMapping.getJavaType(property.getReturnType());

            if (propertyJavaType instanceof Class && ((Class) propertyJavaType).isArray()) {
                Object[] values = (Object[]) value;
                if (values.length == 0) continue;
                value = StringsKt.join(Arrays.asList(values), ",");
            }

            result.add(argument.value());

            if (propertyJavaType == boolean.class || propertyJavaType == Boolean.class) continue;

            if (ParseCommandLineArgumentsKt.isAdvanced(argument)) {
                result.set(result.size() - 1, argument.value() + "=" + value.toString());
            }
            else {
                result.add(value.toString());
            }
        }
    }

    @Nullable
    private static <T> T findInstance(Iterable<? super T> iterable, Class<T> clazz) {
        for (Object item : iterable) {
            if (clazz.isInstance(item)) {
                return clazz.cast(item);
            }
        }
        return null;
    }
}
