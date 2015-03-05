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

package org.jetbrains.kotlin.test;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.io.ZipUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.js.K2JSCompiler;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils;
import org.jetbrains.kotlin.utils.PathUtil;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;

public class MockLibraryUtil {

    private static SoftReference<Class<?>> compiler2JVMClassRef = new SoftReference<Class<?>>(null);
    private static SoftReference<Class<?>> compiler2JSClassRef = new SoftReference<Class<?>>(null);

    @NotNull
    public static File compileLibraryToJar(
            @NotNull String sourcesPath,
            @NotNull String jarName,
            boolean addSources,
            boolean isJsLibrary,
            @NotNull String... extraClasspath
    ) {
        if (isJsLibrary) {
            return compileJsLibraryToJar(sourcesPath, jarName, addSources);
        }
        else {
            return compileLibraryToJar(sourcesPath, jarName, addSources, extraClasspath);
        }
    }

    @NotNull
    public static File compileLibraryToJar(
            @NotNull String sourcesPath,
            @NotNull String jarName,
            boolean addSources,
            @NotNull String... extraClasspath
    ) {
        try {
            File contentDir = JetTestUtils.tmpDir("testLibrary-" + jarName);

            File classesDir = new File(contentDir, "classes");
            compileKotlin(sourcesPath, classesDir, extraClasspath);

            List<File> javaFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.java"), new File(sourcesPath));
            if (!javaFiles.isEmpty()) {
                List<String> classpath = new ArrayList<String>();
                classpath.add(ForTestCompileRuntime.runtimeJarForTests().getPath());
                classpath.add(JetTestUtils.getAnnotationsJar().getPath());
                Collections.addAll(classpath, extraClasspath);

                // Probably no kotlin files were present, so dir might not have been created after kotlin compiler
                if (classesDir.exists()) {
                    classpath.add(classesDir.getPath());
                }
                else {
                    FileUtil.createDirectory(classesDir);
                }

                List<String> options = Arrays.asList(
                        "-classpath", StringUtil.join(classpath, File.pathSeparator),
                        "-d", classesDir.getPath()
                );

                JetTestUtils.compileJavaFiles(javaFiles, options);
            }

            return createJarFile(contentDir, classesDir, sourcesPath, jarName, addSources);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    @NotNull
    public static File compileJsLibraryToJar(
            @NotNull String sourcesPath,
            @NotNull String jarName,
            boolean addSources
    ) {
        try {
            File contentDir = JetTestUtils.tmpDir("testLibrary-" + jarName);

            File outDir = new File(contentDir, "out");
            File outputFile = new File(outDir, jarName + ".js");
            File outputMetaFile = new File(outDir, jarName + ".meta.js");
            compileKotlin2JS(sourcesPath, outputFile, outputMetaFile);

            return createJarFile(contentDir, outDir, sourcesPath, jarName, addSources);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    private static File createJarFile(File contentDir, File dirToAdd, String sourcesPath, String jarName, boolean addSources) throws IOException {
        File jarFile = new File(contentDir, jarName + ".jar");

        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jarFile));
        ZipUtil.addDirToZipRecursively(zip, jarFile, dirToAdd, "", null, null);
        if (addSources) {
            ZipUtil.addDirToZipRecursively(zip, jarFile, new File(sourcesPath), "src", null, null);
        }
        zip.close();

        return jarFile;
    }

    private static void runJvmCompiler(@NotNull List<String> args) {
        runCompiler(getCompiler2JVMClass(), args);
    }

    private static void runJsCompiler(@NotNull List<String> args) {
        runCompiler(getCompiler2JSClass(), args);
    }

    // Runs compiler in custom class loader to avoid effects caused by replacing Application with another one created in compiler.
    private static void runCompiler(@NotNull Class<?> compilerClass, @NotNull List<String> args) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            Object compiler = compilerClass.newInstance();
            Method execMethod = compilerClass.getMethod("exec", PrintStream.class, String[].class);

            Enum<?> invocationResult = (Enum<?>) execMethod.invoke(compiler, new PrintStream(outStream), ArrayUtil.toStringArray(args));

            assertEquals(new String(outStream.toByteArray()), ExitCode.OK.name(), invocationResult.name());
        }
        catch (Throwable e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    public static void compileKotlin(@NotNull String sourcesPath, @NotNull File outDir, @NotNull String... extraClasspath) {
        List<String> classpath = new ArrayList<String>();
        classpath.add(sourcesPath);
        Collections.addAll(classpath, extraClasspath);

        List<String> args = new ArrayList<String>();
        args.add(sourcesPath);
        args.add("-d");
        args.add(outDir.getAbsolutePath());
        args.add("-classpath");
        args.add(StringUtil.join(classpath, File.pathSeparator));

        runJvmCompiler(args);
    }

    public static void compileKotlin2JS(@NotNull String sourcesPath, @NotNull File outputFile, @Nullable File metaFile) {
        List<String> args = new ArrayList<String>();
        if (metaFile != null) {
            args.add("-meta-info");
        }

        args.add("-output");
        args.add(outputFile.getAbsolutePath());

        args.add(sourcesPath);

        runJsCompiler(args);
    }

    public static void compileKotlinModule(@NotNull String modulePath) {
        runJvmCompiler(Arrays.asList("-no-stdlib", "-module", modulePath));
    }

    @NotNull
    private static synchronized Class<?> getCompiler2JVMClass() {
        Class<?> compilerClass = compiler2JVMClassRef.get();
        if (compilerClass == null) {
            compilerClass = getCompilerClass(K2JVMCompiler.class.getName());
            compiler2JVMClassRef = new SoftReference<Class<?>>(compilerClass);
        }
        return compilerClass;
    }

    @NotNull
    private static synchronized Class<?> getCompiler2JSClass() {
        Class<?> compilerClass = compiler2JSClassRef.get();
        if (compilerClass == null) {
            compilerClass = getCompilerClass(K2JSCompiler.class.getName());
            compiler2JSClassRef = new SoftReference<Class<?>>(compilerClass);
        }
        return compilerClass;
    }

    @NotNull
    private static synchronized Class<?> getCompilerClass(String compilerClassName) {
        try {
            File kotlinCompilerJar = new File(PathUtil.getKotlinPathsForDistDirectory().getLibPath(), "kotlin-compiler.jar");
            ClassLoader classLoader =
                    ClassPreloadingUtils.preloadClasses(Collections.singletonList(kotlinCompilerJar), 4096, null, null, null);

            return classLoader.loadClass(compilerClassName);
        }
        catch (Throwable e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    private MockLibraryUtil() {
    }
}
