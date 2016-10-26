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

package org.jetbrains.kotlin.codegen.forTestCompile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class ForTestCompileRuntime {
    private static volatile SoftReference<ClassLoader> reflectJarClassLoader = new SoftReference<ClassLoader>(null);
    private static volatile SoftReference<ClassLoader> runtimeJarClassLoader = new SoftReference<ClassLoader>(null);

    @NotNull
    public static File runtimeJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-runtime.jar"));
    }

    @NotNull
    public static File mockRuntimeJarForTests() {
        return assertExists(new File("dist/kotlin-mock-runtime-for-test.jar"));
    }

    @NotNull
    public static File kotlinTestJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-test.jar"));
    }

    @NotNull
    public static File reflectJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-reflect.jar"));
    }

    @NotNull
    public static File scriptRuntimeJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-script-runtime.jar"));
    }

    // TODO: Do not use these classes, remove them after stdlib tests are merged in the same build as the compiler
    @NotNull
    @Deprecated
    public static File[] runtimeClassesForTests() {
        return new File[] { assertExists(new File("dist/builtins")), assertExists(new File("dist/classes/builtins")), assertExists(new File("dist/classes/stdlib")) };
    }

    @NotNull
    private static File assertExists(@NotNull File file) {
        if (!file.exists()) {
            throw new IllegalStateException(file + " does not exist. Run 'ant dist'");
        }
        return file;
    }

    @NotNull
    public static synchronized ClassLoader runtimeAndReflectJarClassLoader() {
        ClassLoader loader = reflectJarClassLoader.get();
        if (loader == null) {
            loader = createClassLoader(runtimeJarForTests(), reflectJarForTests(), scriptRuntimeJarForTests(), kotlinTestJarForTests());
            reflectJarClassLoader = new SoftReference<ClassLoader>(loader);
        }
        return loader;
    }

    @NotNull
    public static synchronized ClassLoader runtimeJarClassLoader() {
        ClassLoader loader = runtimeJarClassLoader.get();
        if (loader == null) {
            loader = createClassLoader(runtimeJarForTests(), scriptRuntimeJarForTests());
            runtimeJarClassLoader = new SoftReference<ClassLoader>(loader);
        }
        return loader;
    }

    @NotNull
    private static ClassLoader createClassLoader(@NotNull File... files) {
        try {
            List<URL> urls = new ArrayList<URL>(2);
            for (File file : files) {
                urls.add(file.toURI().toURL());
            }
            return new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
        }
        catch (MalformedURLException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }
}
