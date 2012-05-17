/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class CompilerDependencies {

    @NotNull
    private final CompilerSpecialMode compilerSpecialMode;
    @Nullable
    private final File jdkJar;
    @Nullable
    private final File jdkHeadersJar;
    @Nullable
    private final File runtimeJar;

    public CompilerDependencies(@NotNull CompilerSpecialMode compilerSpecialMode, @Nullable File jdkJar, @Nullable File jdkHeadersJar, @Nullable File runtimeJar) {
        this.compilerSpecialMode = compilerSpecialMode;
        this.jdkJar = jdkJar;
        this.jdkHeadersJar = jdkHeadersJar;
        this.runtimeJar = runtimeJar;

        if (compilerSpecialMode.includeJdk()) {
            if (jdkJar == null) {
                throw new IllegalArgumentException("jdk must be included for mode " + compilerSpecialMode);
            }
        }
        if (compilerSpecialMode.includeJdkHeaders()) {
            if (jdkHeadersJar == null) {
                throw new IllegalArgumentException("jdkHeaders must be included for mode " + compilerSpecialMode);
            }
        }
        if (compilerSpecialMode.includeKotlinRuntime()) {
            if (runtimeJar == null) {
                throw new IllegalArgumentException("runtime must be include for mode " + compilerSpecialMode);
            }
        }
    }

    @NotNull
    public CompilerSpecialMode getCompilerSpecialMode() {
        return compilerSpecialMode;
    }

    @Nullable
    public File getJdkJar() {
        return jdkJar;
    }

    @Nullable
    public File getJdkHeadersJar() {
        return jdkHeadersJar;
    }

    @Nullable
    public File getRuntimeJar() {
        return runtimeJar;
    }

    @NotNull
    public List<VirtualFile> getJdkHeaderRoots() {
        if (compilerSpecialMode.includeJdkHeaders()) {
            return Collections.singletonList(PathUtil.jarFileOrDirectoryToVirtualFile(jdkHeadersJar));
        }
        else {
            return Collections.emptyList();
        }
    }

    @NotNull
    public List<VirtualFile> getRuntimeRoots() {
        if (compilerSpecialMode.includeKotlinRuntime()) {
            return Collections.singletonList(PathUtil.jarFileOrDirectoryToVirtualFile(runtimeJar));
        }
        else {
            return Collections.emptyList();
        }
    }

    @NotNull
    public static CompilerDependencies compilerDependenciesForProduction(@NotNull CompilerSpecialMode compilerSpecialMode) {
        return new CompilerDependencies(
                compilerSpecialMode,
                compilerSpecialMode.includeJdk() ? findRtJar() : null,
                compilerSpecialMode.includeJdkHeaders() ? PathUtil.getAltHeadersPath() : null,
                compilerSpecialMode.includeKotlinRuntime() ? PathUtil.getDefaultRuntimePath() : null);
    }

    public static File findRtJar() {
        String javaHome = System.getProperty("java.home");
        if ("jre".equals(new File(javaHome).getName())) {
            javaHome = new File(javaHome).getParent();
        }

        File rtJar = findRtJar(javaHome);

        if (rtJar == null || !rtJar.exists()) {
            throw new IllegalArgumentException("No JDK rt.jar found under " + javaHome);
        }

        return rtJar;
    }

    private static File findRtJar(String javaHome) {
        File rtJar = new File(javaHome, "jre/lib/rt.jar");
        if (rtJar.exists()) {
            return rtJar;
        }

        File classesJar = new File(new File(javaHome).getParentFile().getAbsolutePath(), "Classes/classes.jar");
        if (classesJar.exists()) {
            return classesJar;
        }
        return null;
    }
}
