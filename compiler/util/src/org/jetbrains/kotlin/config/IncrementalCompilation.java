/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.config;

import org.jetbrains.annotations.TestOnly;

import java.util.List;

public class IncrementalCompilation {
    public static final String INCREMENTAL_COMPILATION_JVM_PROPERTY = "kotlin.incremental.compilation";
    public static final String INCREMENTAL_COMPILATION_JS_PROPERTY = "kotlin.incremental.compilation.js";

    public static boolean isEnabledForJvm() {
        return "true".equals(System.getProperty(INCREMENTAL_COMPILATION_JVM_PROPERTY));
    }

    public static boolean isEnabledForJs() {
        return "true".equals(System.getProperty(INCREMENTAL_COMPILATION_JS_PROPERTY));
    }

    @TestOnly
    public static void setIsEnabledForJvm(boolean value) {
        System.setProperty(INCREMENTAL_COMPILATION_JVM_PROPERTY, String.valueOf(value));
    }

    @TestOnly
    public static void setIsEnabledForJs(boolean value) {
        System.setProperty(INCREMENTAL_COMPILATION_JS_PROPERTY, String.valueOf(value));
    }

    public static void toJvmArgs(List<String> jvmArgs) {
        if (isEnabledForJvm()) addJvmSystemFlag(jvmArgs, INCREMENTAL_COMPILATION_JVM_PROPERTY);
        if (isEnabledForJs()) addJvmSystemFlag(jvmArgs, INCREMENTAL_COMPILATION_JS_PROPERTY);
    }

    private static void addJvmSystemFlag(List<String> jvmArgs, String name) {
        jvmArgs.add("D" + name + "=true");
    }
}