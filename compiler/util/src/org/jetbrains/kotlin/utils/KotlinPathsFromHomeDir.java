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

package org.jetbrains.kotlin.utils;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class KotlinPathsFromHomeDir implements KotlinPaths {
    // kotlinc directory
    private final File homePath;

    public KotlinPathsFromHomeDir(@NotNull File homePath) {
        this.homePath = homePath;
    }

    @Override
    @NotNull
    public File getHomePath() {
        return homePath;
    }

    @Override
    @NotNull
    public File getLibPath() {
        return new File(homePath, "lib");
    }

    @Override
    @NotNull
    public File getRuntimePath() {
        return getLibraryFile(PathUtil.KOTLIN_JAVA_RUNTIME_JAR);
    }

    @NotNull
    @Override
    public File getReflectPath() {
        return getLibraryFile(PathUtil.KOTLIN_JAVA_REFLECT_JAR);
    }

    @NotNull
    @Override
    public File getRuntimeSourcesPath() {
        return getLibraryFile(PathUtil.KOTLIN_JAVA_RUNTIME_SRC_JAR);
    }

    @Override
    @NotNull
    public File getJdkAnnotationsPath() {
        return getLibraryFile(PathUtil.JDK_ANNOTATIONS_JAR);
    }

    @NotNull
    @Override
    public File getAndroidSdkAnnotationsPath() {
        return getLibraryFile(PathUtil.ANDROID_SDK_ANNOTATIONS_JAR);
    }

    @Override
    @NotNull
    public File getJsStdLibJarPath() {
        return getLibraryFile(PathUtil.JS_LIB_JAR_NAME);
    }

    @Override
    @NotNull
    public File getJsStdLibSrcJarPath() {
        return getLibraryFile(PathUtil.JS_LIB_SRC_JAR_NAME);
    }

    @NotNull
    private File getLibraryFile(@NotNull String fileName) {
        return new File(getLibPath(), fileName);
    }
}
