/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
    private static volatile SoftReference<ClassLoader> reflectJarClassLoader = new SoftReference<>(null);
    private static volatile SoftReference<ClassLoader> runtimeJarClassLoader = new SoftReference<>(null);

    @NotNull
    public static File runtimeJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-stdlib.jar"));
    }

    @NotNull
    public static File runtimeJarForTestsWithJdk8() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-stdlib-jdk8.jar"));
    }

    @NotNull
    public static File minimalRuntimeJarForTests() {
        return assertExists(new File("dist/kotlin-stdlib-jvm-minimal-for-test.jar"));
    }

    @NotNull
    public static File kotlinTestJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-test.jar"));
    }

    @NotNull
    public static File kotlinTestJUnitJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-test-junit.jar"));
    }

    @NotNull
    public static File kotlinTestJsJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-test-js.jar"));
    }

    @NotNull
    public static File reflectJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-reflect.jar"));
    }

    @NotNull
    public static File scriptRuntimeJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-script-runtime.jar"));
    }

    @NotNull
    public static File runtimeSourcesJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-stdlib-sources.jar"));
    }

    @NotNull
    public static File stdlibMavenSourcesJarForTests() {
        return assertExists(new File("dist/maven/kotlin-stdlib-sources.jar"));
    }

    @NotNull
    public static File stdlibCommonForTests() {
        return assertExists(new File("dist/common/kotlin-stdlib-common.jar"));
    }

    @NotNull
    public static File stdlibCommonSourcesForTests() {
        return assertExists(new File("dist/common/kotlin-stdlib-common-sources.jar"));
    }

    @NotNull
    public static File stdlibJsForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-stdlib-js.klib"));
    }

    @NotNull
    public static File jetbrainsAnnotationsForTests() {
        return assertExists(new File("dist/kotlinc/lib/annotations-13.0.jar"));
    }

    @NotNull
    public static File jvmAnnotationsForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-annotations-jvm.jar"));
    }

    @NotNull
    private static File assertExists(@NotNull File file) {
        if (!file.exists()) {
            throw new IllegalStateException(file + " does not exist. Run 'gradlew dist'");
        }
        return file;
    }

    @NotNull
    public static synchronized ClassLoader runtimeAndReflectJarClassLoader() {
        ClassLoader loader = reflectJarClassLoader.get();
        if (loader == null) {
            loader = createClassLoader(runtimeJarForTests(), reflectJarForTests(), scriptRuntimeJarForTests(), kotlinTestJarForTests());
            reflectJarClassLoader = new SoftReference<>(loader);
        }
        return loader;
    }

    @NotNull
    public static synchronized ClassLoader runtimeJarClassLoader() {
        ClassLoader loader = runtimeJarClassLoader.get();
        if (loader == null) {
            loader = createClassLoader(runtimeJarForTests(), scriptRuntimeJarForTests(), kotlinTestJarForTests());
            runtimeJarClassLoader = new SoftReference<>(loader);
        }
        return loader;
    }

    @NotNull
    private static ClassLoader createClassLoader(@NotNull File... files) {
        try {
            List<URL> urls = new ArrayList<>(2);
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
