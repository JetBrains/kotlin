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

package org.jetbrains.kotlin.generators.di;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public class InjectorGeneratorUtil {
    public static DiType getEffectiveFieldType(Field field) {
        DiType implType = field.getInitialization().getType();
        return implType == null ? field.getType() : implType;
    }

    public static List<Method> getPostConstructMethods(Class<?> clazz) {
        return getInjectSpecialMethods(clazz, PostConstruct.class);
    }

    public static List<Method> getPreDestroyMethods(Class<?> clazz) {
        return getInjectSpecialMethods(clazz, PreDestroy.class);
    }

    private static List<Method> getInjectSpecialMethods(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        List<Method> r = Lists.newArrayList();
        for (Method method : clazz.getMethods()) {
            if (method.getAnnotation(annotationClass) != null) {
                if (method.getParameterTypes().length != 0) {
                    throw new IllegalStateException("@PostConstruct method must have no arguments: " + method);
                }
                r.add(method);
            }
        }
        return r;
    }

    @NotNull
    public static String var(@NotNull DiType type) {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtil.decapitalize(type.getClazz().getSimpleName().replaceFirst("(?<=.)Impl$", "")));
        if (type.getTypeParameters().size() > 0) {
            sb.append("Of");
        }
        for (DiType parameter : type.getTypeParameters()) {
            sb.append(StringUtil.capitalize(var(parameter)));
        }
        return sb.toString();
    }

    private InjectorGeneratorUtil() {}
}
