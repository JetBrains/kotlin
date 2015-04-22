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

package org.jetbrains.kotlin.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

import java.lang.annotation.Annotation;
import java.util.*;

public abstract class JavaToKotlinClassMapBuilder {

    public enum Direction {
        JAVA_TO_KOTLIN,
        KOTLIN_TO_JAVA,
        BOTH
    }

    protected void init() {
        KotlinBuiltIns kotlinBuiltIns = KotlinBuiltIns.getInstance();

        register(Object.class, KotlinBuiltIns.getInstance().getAny());
        register(String.class, kotlinBuiltIns.getString());
        register(CharSequence.class, kotlinBuiltIns.getCharSequence());
        register(Throwable.class, kotlinBuiltIns.getThrowable());
        register(Cloneable.class, kotlinBuiltIns.getCloneable());
        register(Number.class, kotlinBuiltIns.getNumber());
        register(Comparable.class, kotlinBuiltIns.getComparable());
        register(Enum.class, kotlinBuiltIns.getEnum());
        register(Annotation.class, kotlinBuiltIns.getAnnotation());

        register(Iterable.class, kotlinBuiltIns.getIterable(), kotlinBuiltIns.getMutableIterable());
        register(Iterator.class, kotlinBuiltIns.getIterator(), kotlinBuiltIns.getMutableIterator());
        register(Collection.class, kotlinBuiltIns.getCollection(), kotlinBuiltIns.getMutableCollection());
        register(List.class, kotlinBuiltIns.getList(), kotlinBuiltIns.getMutableList());
        register(Set.class, kotlinBuiltIns.getSet(), kotlinBuiltIns.getMutableSet());
        register(Map.class, kotlinBuiltIns.getMap(), kotlinBuiltIns.getMutableMap());
        register(Map.Entry.class, kotlinBuiltIns.getMapEntry(), kotlinBuiltIns.getMutableMapEntry());
        register(ListIterator.class, kotlinBuiltIns.getListIterator(), kotlinBuiltIns.getMutableListIterator());

        register(classId(Deprecated.class), kotlinBuiltIns.getDeprecatedAnnotation(), Direction.JAVA_TO_KOTLIN);

        register(classId(Void.class), kotlinBuiltIns.getNothing(), Direction.KOTLIN_TO_JAVA);
    }

    protected abstract void register(@NotNull ClassId javaClassId, @NotNull ClassDescriptor kotlinDescriptor, @NotNull Direction direction);

    protected abstract void register(
            @NotNull ClassId javaClassId,
            @NotNull ClassDescriptor kotlinDescriptor,
            @NotNull ClassDescriptor kotlinMutableDescriptor
    );

    @NotNull
    private static ClassId classId(@NotNull Class<?> clazz) {
        assert !clazz.isPrimitive() && !clazz.isArray() : "Invalid class: " + clazz;
        Class<?> outer = clazz.getDeclaringClass();
        return outer == null
               ? ClassId.topLevel(new FqName(clazz.getCanonicalName()))
               : classId(outer).createNestedClassId(Name.identifier(clazz.getSimpleName()));
    }

    private void register(@NotNull Class<?> javaClass, @NotNull ClassDescriptor kotlinDescriptor) {
        register(classId(javaClass), kotlinDescriptor, Direction.BOTH);
    }

    private void register(
            @NotNull Class<?> javaClass,
            @NotNull ClassDescriptor kotlinDescriptor,
            @NotNull ClassDescriptor kotlinMutableDescriptor
    ) {
        register(classId(javaClass), kotlinDescriptor, kotlinMutableDescriptor);
    }
}
