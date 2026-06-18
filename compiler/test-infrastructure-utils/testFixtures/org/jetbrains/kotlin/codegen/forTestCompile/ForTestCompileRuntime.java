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
import java.util.*;

import static java.lang.System.*;
import static org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.*;

public class ForTestCompileRuntime {
    private static volatile SoftReference<ClassLoader> reflectJarClassLoader = new SoftReference<>(null);
    private static volatile SoftReference<ClassLoader> runtimeJarClassLoader = new SoftReference<>(null);

    @NotNull
    public static File runtimeJarForTests() {
        return getFileFromProperty(KOTLIN_FULL_STDLIB_PATH);
    }

    @NotNull
    public static File thirdPartyJsr305ForTests() {
        return getFileFromProperty(KOTLIN_THIRDPARTY_JSR305_PATH);
    }

    @NotNull
    public static File thirdPartyJava8AnnotationsForTests() {
        return getFileFromProperty(KOTLIN_THIRDPARTY_JAVA8_ANNOTATIONS_PATH);
    }

    /**
     * This function left as a workaround for AbstractJavaModulesIntegrationTest
     * For any other case use `runtimeJarForTests` instead
     */
    @NotNull
    @Deprecated
    public static File runtimeJarFromDistForTests() {
        return new File("dist/kotlinc/lib/kotlin-stdlib.jar");
    }

    @NotNull
    public static File runtimeJarForTestsWithJdk8() {
        return getFileFromProperty(KOTLIN_FULL_STDLIB_PATH);
    }

    @NotNull
    public static File minimalRuntimeJarForTests() {
        return getFileFromProperty(KOTLIN_MINIMAL_STDLIB_PATH);
    }

    @NotNull
    public static File kotlinTestJarForTests() {
        return getFileFromProperty(KOTLIN_TEST_JAR_PATH);
    }

    @NotNull
    public static File reflectJarForTests() {
        return getFileFromProperty(KOTLIN_REFLECT_JAR_PATH);
    }

    /**
     * This function left as a workaround for AbstractJavaModulesIntegrationTest
     * For any other case use `runtimeJarForTests` instead
     */
    @NotNull
    @Deprecated
    public static File reflectJarFromDistForTests() {
        return new File("dist/kotlinc/lib/kotlin-reflect.jar");
    }

    @NotNull
    public static File scriptRuntimeJarForTests() {
        return getFileFromProperty(KOTLIN_SCRIPT_RUNTIME_PATH);
    }

    @NotNull
    public static File distKotlincForTests() {
        return new File(getFileFromProperty(KOTLIN_DIST_PATH), "kotlinc");
    }

    @NotNull
    public static File pluginSandboxAnnotationsJvmForTests() {
        return getFileFromProperty(
                PLUGIN_SANDBOX_ANNOTATIONS_JAR_PATH
        );
    }

    @NotNull
    public static File pluginSandboxAnnotationsJsForTests() {
        return getFileFromProperty(
                PLUGIN_SANDBOX_ANNOTATIONS_JS_KLIB_PATH
        );
    }

    @NotNull
    public static File pluginSandboxAnnotationsWasmForTests() {
        return getFileFromProperty(
                PLUGIN_SANDBOX_ANNOTATIONS_WASM_KLIB_PATH
        );
    }

    @NotNull
    public static File pluginSandboxJarForTests() {
        return getFileFromProperty(
                PLUGIN_SANDBOX_JAR_PATH
        );
    }

    @NotNull
    public static List<File> scriptingPluginFilesForTests() {
        return getFilesFromProperty(KOTLIN_SCRIPTING_PLUGIN_CLASSPATH);
    }

    @NotNull
    public static List<File> testScriptDefinitionClasspathForTests() {
        return getFilesFromProperty(KOTLIN_TEST_SCRIPT_DEFINITION_CLASSPATH);
    }

    @NotNull
    public static File runtimeSourcesJarForTests() {
        return getFileFromProperty(KOTLIN_FULL_STDLIB_SOURCES_PATH);
    }

    @NotNull
    public static File stdlibCommonForTests() {
        return getFileFromProperty(KOTLIN_COMMON_STDLIB_PATH);
    }

    @NotNull
    public static File stdlibJs() {
        return stdlibJsForTests();
    }

    @NotNull
    public static File stdlibJsReducedForTests() {
        return getFileFromProperty(KOTLIN_JS_REDUCED_STDLIB_PATH);
    }

    @NotNull
    public static File kotlinTestJsKLibForTests() {
        return getFileFromProperty(KOTLIN_JS_KOTLIN_TEST_KLIB_PATH);
    }

    @NotNull
    public static File fullWasmStdlibForTests(String alias) {
        return getFileFromProperty("kotlin." + alias + ".stdlib.path");
    }

    @NotNull
    public static File kotlinTestWasmKLibForTests(String alias) {
        return getFileFromProperty("kotlin." + alias + ".kotlin.test.path");
    }

    @NotNull
    private static List<File> getFilesFromProperty(String property) {
        String classpathString = getProperty(property, null);
        if (classpathString == null) {
            throw new IllegalStateException("Property " + property + " is missing");
        }

        List<File> list = new ArrayList<>();
        for (String classpathEntryString : classpathString.split(File.pathSeparator)) {
            File file = new File(classpathEntryString);
            if (!file.exists()) {
                throw new IllegalStateException("Property " + property + " contains non-existent path: " + classpathEntryString);
            }
            list.add(file);
        }

        return list;
    }

    public static File getFileFromProperty(String property) {
        String path = getProperty(property);
        assert (path != null) : "Property " + property + " is not defined";
        File file = new File(path);
        assert (file.exists()) : path + " doesn't exist; property: " + property;
        return file;
    }

    @NotNull
    public static File transformTestDataPath(String path) {
        String property = getProperty(KOTLIN_TESTDATA_ROOTS);
        if (property != null) {
            @NotNull String[] roots = property.split(";");
            for (String root : roots) {
                String relativePath = root.substring(0, root.indexOf('='));
                String absolutePath = root.substring(root.indexOf('=') + 1);

                if (path.startsWith(relativePath)) {
                    return new File(path.replace(relativePath, absolutePath));
                }
            }
        }
        return new File(path);
    }

    @NotNull
    public static File jvmAnnotationsForTests() {
        return getFileFromProperty(KOTLIN_ANNOTATIONS_PATH);
    }

    @NotNull
    public static File stdlibJsForTests() {
        return getFileFromProperty(KOTLIN_JS_STDLIB_KLIB_PATH);
    }

    public static File stdlibWebForTests() {
        return getFileFromProperty(KOTLIN_WEB_STDLIB_KLIB_PATH);
    }

    @NotNull
    public static File stdlibWasmJsForTests() {
        return getFileFromProperty(KOTLIN_WASM_STDLIB_KLIB_PATH);
    }

    @NotNull
    public static File thirdPartyAnnotations() {
        return getFileFromProperty(KOTLIN_THIRDPARTY_ANNOTATIONS_PATH);
    }

    @NotNull
    public static File kotlinNativeImageDistForTests() {
        return getFileFromProperty(KOTLIN_NATIVE_IMAGE_DIST_PATH);
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

    public static File lombokCompilerPluginForTests() {
        return getFileFromProperty(LOMBOK_COMPILER_PLUGIN_JAR_PATH);
    }

    public static File allOpenCompilerPluginForTests() {
        return getFileFromProperty(ALLOPEN_COMPILER_PLUGIN_JAR_PATH);
    }

    public static File noArgCompilerPluginForTests() {
        return getFileFromProperty(NOARG_COMPILER_PLUGIN_JAR_PATH);
    }

    public static @NotNull File mainKtsJar() {
        return getFileFromProperty(MAIN_KTS_JAR_PATH);
    }
}
