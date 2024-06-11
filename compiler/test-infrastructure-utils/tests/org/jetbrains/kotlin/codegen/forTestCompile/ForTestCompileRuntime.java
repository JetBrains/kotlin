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

import static java.lang.System.getProperty;
import static org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.*;

public class ForTestCompileRuntime {
    private static volatile SoftReference<ClassLoader> reflectJarClassLoader = new SoftReference<>(null);
    private static volatile SoftReference<ClassLoader> runtimeJarClassLoader = new SoftReference<>(null);

    @NotNull
    public static File runtimeJarForTests() {
        return propertyOrDist(KOTLIN_FULL_STDLIB_PATH, "dist/kotlinc/lib/kotlin-stdlib.jar");
    }

    @NotNull
    public static File runtimeJarForTestsWithJdk8() {
        return propertyOrDist(KOTLIN_FULL_STDLIB_PATH, "dist/kotlinc/lib/kotlin-stdlib-jdk8.jar");
    }

    @NotNull
    public static File minimalRuntimeJarForTests() {
        return assertExists(new File("dist/kotlin-stdlib-jvm-minimal-for-test.jar"));
    }

    @NotNull
    public static File kotlinTestJarForTests() {
        return propertyOrDist(KOTLIN_TEST_JAR_PATH, "dist/kotlinc/lib/kotlin-test.jar");
    }

    @NotNull
    public static File reflectJarForTests() {
        return propertyOrDist(KOTLIN_REFLECT_JAR_PATH, "dist/kotlinc/lib/kotlin-reflect.jar");
    }

    @NotNull
    public static File scriptRuntimeJarForTests() {
        return propertyOrDist(KOTLIN_SCRIPT_RUNTIME_PATH, "dist/kotlinc/lib/kotlin-script-runtime.jar");
    }

    @NotNull
    public static File runtimeSourcesJarForTests() {
        return assertExists(new File("dist/kotlinc/lib/kotlin-stdlib-sources.jar"));
    }

    @NotNull
    public static File stdlibCommonForTests() {
        return propertyOrDist(KOTLIN_COMMON_STDLIB_PATH, "dist/common/kotlin-stdlib-common.klib");
    }

    private static File propertyOrDist(String property, String distPath) {
        String path = getProperty(property, distPath);
        File file = new File(path);
        assert(file.exists()) : path + " doesn't exist";
        return file;
    }

    @NotNull
    public static File jvmAnnotationsForTests() {
        return propertyOrDist(KOTLIN_ANNOTATIONS_PATH, "dist/kotlinc/lib/kotlin-annotations-jvm.jar");
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
