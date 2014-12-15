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

package org.jetbrains.jet.compiler.runner;

import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.preloading.ClassCondition;
import org.jetbrains.jet.preloading.ClassPreloadingUtils;
import org.jetbrains.jet.utils.KotlinPaths;

import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.jet.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.ERROR;

public class CompilerRunnerUtil {

    private static SoftReference<ClassLoader> ourClassLoaderRef = new SoftReference<ClassLoader>(null);

    @Nullable
    private static File getLibPath(@NotNull KotlinPaths paths, @NotNull MessageCollector messageCollector) {
        File libs = paths.getLibPath();
        if (libs.exists() && !libs.isFile()) return libs;

        messageCollector.report(
                ERROR,
                "Broken compiler at '" + libs.getAbsolutePath() + "'. Make sure plugin is properly installed",
                NO_LOCATION
        );

        return null;
    }

    @NotNull
    private static List<File> compilerClasspath(@NotNull File libs) {
        return Arrays.asList(
                new File(libs, "kotlin-compiler.jar"),
                new File(libs, "kotlin-runtime.jar")
        );
    }

    @NotNull
    private static ClassLoader createPreloader(
            @NotNull File libPath,
            @Nullable ClassLoader parentClassLoader,
            @Nullable ClassCondition classToLoadByParent
    ) throws IOException {
        return ClassPreloadingUtils.preloadClasses(
                compilerClasspath(libPath), /* estimatedClassNumber = */ 4096, parentClassLoader, classToLoadByParent
        );
    }

    @NotNull
    private static URLClassLoader createClassLoader(@NotNull File libPath) throws MalformedURLException {
        List<File> jars = compilerClasspath(libPath);
        URL[] urls = new URL[jars.size()];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = jars.get(i).toURI().toURL();
        }
        return new URLClassLoader(urls, null);
    }

    private static void handleProcessTermination(int exitCode, @NotNull MessageCollector messageCollector) {
        if (exitCode != 0 && exitCode != 1) {
            messageCollector.report(ERROR, "Compiler terminated with exit code: " + exitCode, NO_LOCATION);
        }
    }

    public static int getReturnCodeFromObject(@Nullable Object rc) throws Exception {
        if (rc == null) {
            return /* ExitCode.INTERNAL_ERROR */ 2;
        }
        else if ("org.jetbrains.jet.cli.common.ExitCode".equals(rc.getClass().getCanonicalName())) {
            return (Integer) rc.getClass().getMethod("getCode").invoke(rc);
        }
        else {
            throw new IllegalStateException("Unexpected return: " + rc);
        }
    }

    @Nullable
    public static Object invokeExecMethod(
            @NotNull String compilerClassName,
            @NotNull String[] arguments,
            @NotNull CompilerEnvironment environment,
            @NotNull MessageCollector messageCollector,
            @NotNull PrintStream out,
            boolean usePreloader
    ) throws Exception {
        File libPath = getLibPath(environment.getKotlinPaths(), messageCollector);
        if (libPath == null) return null;

        ClassLoader classLoader = ourClassLoaderRef.get();
        if (classLoader == null) {
            classLoader = usePreloader
                          ? createPreloader(libPath, environment.getParentClassLoader(), environment.getClassesToLoadByParent())
                          : createClassLoader(libPath);
            ourClassLoaderRef = new SoftReference<ClassLoader>(classLoader);
        }

        Class<?> kompiler = Class.forName(compilerClassName, true, classLoader);
        Method exec = kompiler.getMethod(
                "execAndOutputXml",
                PrintStream.class,
                Class.forName("org.jetbrains.jet.config.Services", true, classLoader),
                String[].class
        );

        return exec.invoke(kompiler.newInstance(), out, environment.getServices(), arguments);
    }

    public static void outputCompilerMessagesAndHandleExitCode(
            @NotNull MessageCollector messageCollector,
            @NotNull OutputItemsCollector outputItemsCollector,
            @NotNull Function<PrintStream, Integer> compilerRun
    ) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outputStream);

        int exitCode = compilerRun.fun(out);

        BufferedReader reader = new BufferedReader(new StringReader(outputStream.toString()));
        CompilerOutputParser.parseCompilerMessagesFromReader(messageCollector, reader, outputItemsCollector);
        handleProcessTermination(exitCode, messageCollector);
    }
}
