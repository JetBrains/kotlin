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

package org.jetbrains.kotlin.compilerRunner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils;
import org.jetbrains.kotlin.preloading.Preloader;
import org.jetbrains.kotlin.utils.KotlinPaths;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR;

public class CompilerRunnerUtil {

    private static SoftReference<ClassLoader> ourClassLoaderRef = new SoftReference<ClassLoader>(null);

    @NotNull
    private static synchronized ClassLoader getOrCreateClassLoader(
            @NotNull CompilerEnvironment environment,
            @NotNull File compilerJar
    ) throws IOException {
        ClassLoader classLoader = ourClassLoaderRef.get();
        if (classLoader == null) {
            classLoader = ClassPreloadingUtils.preloadClasses(
                    Collections.singletonList(compilerJar),
                    Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE,
                    CompilerRunnerUtil.class.getClassLoader(),
                    environment.getClassesToLoadByParent()
            );
            ourClassLoaderRef = new SoftReference<ClassLoader>(classLoader);
        }
        return classLoader;
    }

    @Nullable
    public static File getCompilerPath(@NotNull KotlinPaths paths, @NotNull MessageCollector messageCollector) {
        File compilerPath = paths.getCompilerPath();
        // TODO: use more reliable criteria
        if (compilerPath.exists() && compilerPath.isFile()) return compilerPath;

        messageCollector.report(
                ERROR,
                "Broken compiler at '" + compilerPath.getAbsolutePath() + "'. Make sure plugin is properly installed",
                NO_LOCATION
        );

        return null;
    }

    @Nullable
    public static Object invokeExecMethod(
            @NotNull String compilerClassName,
            @NotNull String[] arguments,
            @NotNull CompilerEnvironment environment,
            @NotNull MessageCollector messageCollector,
            @NotNull PrintStream out
    ) throws Exception {
        // TODO: after conversion to kotlin, reimplement getOrCreateClassLoader to accept compilerPath generator, to calculate compilerPath lazily
        File compilerPath = getCompilerPath(environment.getKotlinPaths(), messageCollector);
        if (compilerPath == null) return null;

        ClassLoader classLoader = getOrCreateClassLoader(environment, compilerPath);

        Class<?> kompiler = Class.forName(compilerClassName, true, classLoader);
        Method exec = kompiler.getMethod(
                "execAndOutputXml",
                PrintStream.class,
                Class.forName("org.jetbrains.kotlin.config.Services", true, classLoader),
                String[].class
        );

        return exec.invoke(kompiler.newInstance(), out, environment.getServices(), arguments);
    }
}
