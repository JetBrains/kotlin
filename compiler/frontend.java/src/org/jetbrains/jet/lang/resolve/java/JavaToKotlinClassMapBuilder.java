/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author svtk
 */
public abstract class JavaToKotlinClassMapBuilder {

    /*package*/ void init() {
        JetStandardLibrary standardLibrary = JetStandardLibrary.getInstance();

        register(Object.class, KotlinBuiltIns.getInstance().getAny());
        register(String.class, standardLibrary.getString());
        register(CharSequence.class, standardLibrary.getCharSequence());
        register(Throwable.class, standardLibrary.getThrowable());
        register(Number.class, standardLibrary.getNumber());
        register(Comparable.class, standardLibrary.getComparable());
        register(Enum.class, standardLibrary.getEnum());
        register(Annotation.class, standardLibrary.getAnnotation());

        register(Iterable.class, standardLibrary.getIterable(), standardLibrary.getMutableIterable());
        register(Iterator.class, standardLibrary.getIterator(), standardLibrary.getMutableIterator());
        register(Collection.class, standardLibrary.getCollection(), standardLibrary.getMutableCollection());
        register(List.class, standardLibrary.getList(), standardLibrary.getMutableList());
        register(Set.class, standardLibrary.getSet(), standardLibrary.getMutableSet());
        register(Map.class, standardLibrary.getMap(), standardLibrary.getMutableMap());
        register(Map.Entry.class, standardLibrary.getMapEntry(), standardLibrary.getMutableMapEntry());
        register(ListIterator.class, standardLibrary.getListIterator(), standardLibrary.getMutableListIterator());
    }

    /*package*/ abstract void register(@NotNull Class<?> javaClass, @NotNull ClassDescriptor kotlinDescriptor);

    /*package*/ abstract void register(@NotNull Class<?> javaClass, @NotNull ClassDescriptor kotlinDescriptor, @NotNull ClassDescriptor kotlinMutableDescriptor);
}
