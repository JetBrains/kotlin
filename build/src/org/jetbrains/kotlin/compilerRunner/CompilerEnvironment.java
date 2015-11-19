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
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.config.Services;
import org.jetbrains.kotlin.preloading.ClassCondition;
import org.jetbrains.kotlin.utils.KotlinPaths;
import org.jetbrains.kotlin.utils.PathUtil;

import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR;

public class CompilerEnvironment {
    @NotNull
    public static CompilerEnvironment getEnvironmentFor(
            @NotNull KotlinPaths kotlinPaths,
            @NotNull ClassCondition classesToLoadByParent,
            @NotNull Services compilerServices
    ) {
        return new CompilerEnvironment(kotlinPaths, classesToLoadByParent, compilerServices);
    }

    private final KotlinPaths kotlinPaths;
    private final ClassCondition classesToLoadByParent;
    private final Services services;

    private CompilerEnvironment(
            @NotNull KotlinPaths kotlinPaths,
            @NotNull ClassCondition classesToLoadByParent,
            @NotNull Services services
    ) {
        this.kotlinPaths = kotlinPaths;
        this.classesToLoadByParent = classesToLoadByParent;
        this.services = services;
    }

    public boolean success() {
        return kotlinPaths.getHomePath().exists();
    }

    @NotNull
    public KotlinPaths getKotlinPaths() {
        return kotlinPaths;
    }

    @NotNull
    public ClassCondition getClassesToLoadByParent() {
        return classesToLoadByParent;
    }

    public void reportErrorsTo(@NotNull MessageCollector messageCollector) {
        if (!kotlinPaths.getHomePath().exists()) {
            messageCollector.report(ERROR, "Cannot find kotlinc home: " + kotlinPaths.getHomePath() + ". Make sure plugin is properly installed, " +
                                           "or specify " + PathUtil.JPS_KOTLIN_HOME_PROPERTY + " system property", NO_LOCATION);
        }
    }

    @NotNull
    public Services getServices() {
        return services;
    }
}
