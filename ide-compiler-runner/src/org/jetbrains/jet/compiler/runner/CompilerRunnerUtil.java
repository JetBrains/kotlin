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
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.preloading.ClassCondition;
import org.jetbrains.jet.preloading.ClassPreloadingUtils;
import org.jetbrains.jet.utils.KotlinPaths;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.jetbrains.jet.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.ERROR;

public class CompilerRunnerUtil {

    private static SoftReference<ClassLoader> ourClassLoaderRef = new SoftReference<ClassLoader>(null);

    @NotNull
    private static synchronized ClassLoader getOrCreateClassLoader(
            @NotNull CompilerEnvironment environment,
            @NotNull File libPath
    ) throws IOException {
        ClassLoader classLoader = ourClassLoaderRef.get();
        if (classLoader == null) {
            classLoader = createClassLoader(libPath, environment.getParentClassLoader(), environment.getClassesToLoadByParent());
            ourClassLoaderRef = new SoftReference<ClassLoader>(classLoader);
        }
        return classLoader;
    }

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
    private static File compilerJar(@NotNull File libPath) {
        return new File(libPath, "kotlin-compiler.jar");
    }

    @NotNull
    private static String loadCompilerClasspathSpaceSeparated(@NotNull File libPath) throws IOException {
        JarFile jar = new JarFile(compilerJar(libPath));
        try {
            return (String) jar.getManifest().getMainAttributes().get(Attributes.Name.CLASS_PATH);
        }
        finally {
            jar.close();
        }
    }

    @NotNull
    private static ClassLoader createClassLoader(
            @NotNull final File libPath,
            @Nullable ClassLoader parentClassLoader,
            @Nullable ClassCondition classToLoadByParent
    ) throws IOException {
        List<URL> classpath = KotlinPackage.map(loadCompilerClasspathSpaceSeparated(libPath).split(" "), new Function1<String, URL>() {
            @Override
            public URL invoke(String dependency) {
                try {
                    return new File(libPath, dependency).toURI().toURL();
                }
                catch (MalformedURLException e) {
                    throw UtilsPackage.rethrow(e);
                }
            }
        });

        return ClassPreloadingUtils.preloadClasses(
                Collections.singletonList(compilerJar(libPath)),
                /* estimatedClassNumber = */ 4096,
                new URLClassLoader(classpath.toArray(new URL[classpath.size()]), parentClassLoader),
                classToLoadByParent
        );
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
            @NotNull PrintStream out
    ) throws Exception {
        File libPath = getLibPath(environment.getKotlinPaths(), messageCollector);
        if (libPath == null) return null;

        ClassLoader classLoader = getOrCreateClassLoader(environment, libPath);

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
