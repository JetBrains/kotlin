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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
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
    @NotNull
    private final File[] altHeadersClasspath;
    @Nullable
    private final File runtimeJar;

    public CompilerDependencies(@NotNull CompilerSpecialMode compilerSpecialMode, @Nullable File jdkJar, @NotNull File[] altHeadersClasspath, @Nullable File runtimeJar) {
        this.compilerSpecialMode = compilerSpecialMode;
        this.jdkJar = jdkJar;
        this.altHeadersClasspath = altHeadersClasspath;
        this.runtimeJar = runtimeJar;

        if (compilerSpecialMode.includeJdk()) {
            if (jdkJar == null) {
                throw new IllegalArgumentException("jdk must be included for mode " + compilerSpecialMode);
            }
        }
        if (compilerSpecialMode.includeAltHeaders()) {
            if (altHeadersClasspath.length == 0) {
                throw new IllegalArgumentException("altHeaders must be included for mode " + compilerSpecialMode);
            }
            for (int i = 0; i < altHeadersClasspath.length; i++) {
                File file = altHeadersClasspath[i];
                if (file == null) {
                    throw new IllegalArgumentException("altHeaders file " + i + " must not be null for included for mode " + compilerSpecialMode);
                }
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

    @NotNull
    public File[] getAltHeadersClassPath() {
        return altHeadersClasspath;
    }

    @Nullable
    public File getRuntimeJar() {
        return runtimeJar;
    }

    @NotNull
    public List<VirtualFile> getAltHeaderRoots() {
        if (compilerSpecialMode.includeAltHeaders()) {
            return ContainerUtil.map2List(altHeadersClasspath, new Function<File, VirtualFile>() {
                @Override
                public VirtualFile fun(File file) {
                    if (file.exists()) {
                        if (file.isDirectory()) {
                            return VirtualFileManager.getInstance()
                                    .findFileByUrl("file://" + FileUtil.toSystemIndependentName(file.getAbsolutePath()));
                        }
                        else {
                            return PathUtil.jarFileToVirtualFile(file);
                        }
                    }
                    else {
                        throw new IllegalStateException("Path " + file + " does not exist.");
                    }
                }
            });
        }
        else {
            return Collections.emptyList();
        }
    }

    @NotNull
    public List<VirtualFile> getRuntimeRoots() {
        if (compilerSpecialMode.includeKotlinRuntime()) {
            return Collections.singletonList(PathUtil.jarFileToVirtualFile(runtimeJar));
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
                compilerSpecialMode.includeAltHeaders() ? new File[]{PathUtil.getAltHeadersPath()} : new File[0],
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
