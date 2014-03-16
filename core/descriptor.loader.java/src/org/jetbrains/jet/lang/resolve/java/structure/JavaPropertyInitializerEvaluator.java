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

package org.jetbrains.jet.lang.resolve.java.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;

import java.util.Iterator;
import java.util.ServiceLoader;

public abstract class JavaPropertyInitializerEvaluator {
    private static JavaPropertyInitializerEvaluator instance;

    @NotNull
    public static JavaPropertyInitializerEvaluator getInstance() {
        if (instance == null) {
            Iterator<JavaPropertyInitializerEvaluator> iterator =
                    ServiceLoader.load(JavaPropertyInitializerEvaluator.class, JavaPropertyInitializerEvaluator.class.getClassLoader()).iterator();
            assert iterator.hasNext() : "No service found: " + JavaPropertyInitializerEvaluator.class.getName();
            instance = iterator.next();
        }
        return instance;
    }

    @Nullable
    public abstract CompileTimeConstant<?> getInitializerConstant(@NotNull JavaField field, @NotNull PropertyDescriptor descriptor);
}
