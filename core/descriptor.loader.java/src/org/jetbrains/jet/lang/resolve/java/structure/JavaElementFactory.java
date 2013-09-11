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

package org.jetbrains.jet.lang.resolve.java.structure;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.ServiceLoader;

public abstract class JavaElementFactory {
    private static JavaElementFactory instance;

    @NotNull
    public static JavaElementFactory getInstance() {
        if (instance == null) {
            Iterator<JavaElementFactory> iterator =
                    ServiceLoader.load(JavaElementFactory.class, JavaElementFactory.class.getClassLoader()).iterator();
            assert iterator.hasNext() : "No service found: " + JavaElementFactory.class.getName();
            instance = iterator.next();
        }
        return instance;
    }

    @NotNull
    public abstract JavaArrayType createArrayType(@NotNull JavaType elementType);
}
