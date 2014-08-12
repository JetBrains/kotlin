/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.preloading.ClassLoaderFactory;
import org.jetbrains.jet.utils.KotlinPaths;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.ERROR;

public final class CompilerEnvironment {

    public static CompilerEnvironment getEnvironmentFor(
            @NotNull KotlinPaths kotlinPaths,
            @Nullable File outputDir,
            @Nullable ClassLoaderFactory parentFactory,
            @NotNull Object... serviceImplementations
    ) {
        Map<Class, Object> servicesMap = new HashMap<Class, Object>();
        for (Object serviceImplementation : serviceImplementations) {
            for (Class<?> serviceInterface : serviceImplementation.getClass().getInterfaces()) {
                servicesMap.put(serviceInterface, serviceImplementation);
            }
        }
        return new CompilerEnvironment(kotlinPaths, outputDir, parentFactory, servicesMap);
    }

    @NotNull
    private final KotlinPaths kotlinPaths;
    @Nullable
    private final File output;
    @Nullable
    private final ClassLoaderFactory parentFactory;
    @NotNull
    private final Map<Class, Object> services;

    private CompilerEnvironment(
            @NotNull KotlinPaths kotlinPaths,
            @Nullable File output,
            @Nullable ClassLoaderFactory parentFactory,
            @NotNull Map<Class, Object> services
    ) {
        this.kotlinPaths = kotlinPaths;
        this.output = output;
        this.parentFactory = parentFactory;
        this.services = services;
    }

    public boolean success() {
        return kotlinPaths.getHomePath().exists() && output != null;
    }

    @NotNull
    public KotlinPaths getKotlinPaths() {
        return kotlinPaths;
    }

    @NotNull
    public File getOutput() {
        assert output != null;
        return output;
    }

    @Nullable
    public ClassLoaderFactory getParentFactory() {
        return parentFactory;
    }

    public void reportErrorsTo(@NotNull MessageCollector messageCollector) {
        if (output == null) {
            messageCollector.report(ERROR, "[Internal Error] No output directory", NO_LOCATION);
        }
        if (!kotlinPaths.getHomePath().exists()) {
            messageCollector.report(ERROR, "Cannot find kotlinc home: " + kotlinPaths.getHomePath() + ". Make sure plugin is properly installed, " +
                                           "or specify " + PathUtil.JPS_KOTLIN_HOME_PROPERTY + " system property", NO_LOCATION);
        }
    }

    @NotNull
    public Map<Class, Object> getServices() {
        return services;
    }
}
