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

import java.io.File;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import static org.jetbrains.kotlin.utils.UtilsPackage.rethrow;

public class ForTestCompileRuntime {
    private static volatile SoftReference<ClassLoader> runtimeJarClassLoader = new SoftReference<ClassLoader>(null);

    @NotNull
    public static File runtimeJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-runtime.jar"));
    }

    @NotNull
    public static File reflectJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-reflect.jar"));
    }

    @NotNull
    private static File assertExists(@NotNull File file) {
        if (!file.exists()) {
            throw new IllegalStateException(file + " does not exist. Run 'ant dist'");
        }
        return file;
    }

    @NotNull
    public static synchronized ClassLoader runtimeJarClassLoader() {
        ClassLoader loader = runtimeJarClassLoader.get();
        if (loader == null) {
            try {
                loader = new URLClassLoader(new URL[] {
                        runtimeJarForTests().toURI().toURL(),
                        reflectJarForTests().toURI().toURL()
                }, null);
            }
            catch (MalformedURLException e) {
                throw rethrow(e);
            }
            runtimeJarClassLoader = new SoftReference<ClassLoader>(loader);
        }

        return loader;
    }
}
