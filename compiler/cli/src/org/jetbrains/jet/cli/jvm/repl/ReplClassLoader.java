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

package org.jetbrains.jet.cli.jvm.repl;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.util.Map;

/**
 * @author Stepan Koltsov
 */
public class ReplClassLoader extends ClassLoader {

    private Map<JvmClassName, byte[]> classes = Maps.newLinkedHashMap();

    public ReplClassLoader(@NotNull ClassLoader parent) {
        super(parent);
    }

    public ReplClassLoader() {
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] classBytes = classes.get(JvmClassName.byFqNameWithoutInnerClasses(name));
        if (classBytes != null) {
            return defineClass(name, classBytes, 0, classBytes.length);
        }
        else {
            return super.findClass(name);
        }
    }

    public void addClass(@NotNull JvmClassName className, @NotNull byte[] bytes) {
        byte[] oldBytes = classes.put(className, bytes);
        if (oldBytes != null) {
            throw new IllegalStateException("Rewrite at key " + className);
        }
    }

    public void dumpClasses(@NotNull PrintWriter writer) {
        for (byte[] classBytes : classes.values()) {
            new ClassReader(classBytes).accept(new TraceClassVisitor(writer), 0);
        }
    }

}
