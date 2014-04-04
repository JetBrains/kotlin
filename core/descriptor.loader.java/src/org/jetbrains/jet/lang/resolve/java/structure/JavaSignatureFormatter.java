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

public abstract class JavaSignatureFormatter {
    private static JavaSignatureFormatter instance;

    @NotNull
    public static JavaSignatureFormatter getInstance() {
        if (instance == null) {
            Iterator<JavaSignatureFormatter> iterator =
                    ServiceLoader.load(JavaSignatureFormatter.class, JavaSignatureFormatter.class.getClassLoader()).iterator();
            assert iterator.hasNext() : "No service found: " + JavaSignatureFormatter.class.getName();
            instance = iterator.next();
        }
        return instance;
    }

    /**
     * @return a formatted signature of a method, showing method name and fully qualified names of its parameter types, e.g.:
     * {@code "foo(double, java.lang.String)"}
     */
    @NotNull
    public abstract String formatMethod(@NotNull JavaMethod method);
}
