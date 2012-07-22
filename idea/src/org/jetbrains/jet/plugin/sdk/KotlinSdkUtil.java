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

package org.jetbrains.jet.plugin.sdk;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author Maxim.Manuylov
 *         Date: 19.05.12
 */
public class KotlinSdkUtil {
    @NotNull private static final PersistentLibraryKind<KotlinSdkProperties> KOTLIN_SDK_KIND =
            new PersistentLibraryKind<KotlinSdkProperties>("KotlinSDK", false) {
                @NotNull
                @Override
                public KotlinSdkProperties createDefaultProperties() {
                    return new KotlinSdkProperties("");
                }
            };
    @NotNull private static final String[] KOTLIN_COMPILER_JAR_ENTRY_NAMES = {
        "org/jetbrains/jet/cli/KotlinCompiler.class",
        "org/jetbrains/jet/cli/jvm/K2JVMCompiler.class"
    };

    private KotlinSdkUtil() {}

    public static boolean isSDKHome(@Nullable final VirtualFile dir) {
        return dir != null && isSDKHome(new File(dir.getPath()));
    }

    private static boolean isSDKHome(@NotNull final File dir) {
        return dir.isDirectory() && isKotlinCompilerJar(PathUtil.getCompilerPath(dir));
    }

    @Nullable
    public static String getSDKVersion(@NotNull final String sdkHomePath) {
        final String buildNumber = getSDKBuildNumber(sdkHomePath);
        if (buildNumber == null) return null;
        final int lastDotPos = buildNumber.lastIndexOf('.');
        return lastDotPos == -1 ? buildNumber : buildNumber.substring(0, lastDotPos);
    }

    @Nullable
    private static String getSDKBuildNumber(@NotNull final String sdkHomePath) {
        try {
            return FileUtil.loadFile(new File(sdkHomePath, "build.txt")).trim();
        }
        catch (final IOException e) {
            try {
                final File compilerJar = PathUtil.getCompilerPath(new File(sdkHomePath));
                return compilerJar == null ? null : getJarImplementationVersion(compilerJar);
            }
            catch (final IOException e1) {
                return null;
            }
        }
    }

    @Nullable
    private static String getJarImplementationVersion(@NotNull final File jar) throws IOException {
        final JarFile jarFile = new JarFile(jar);
        try {
            final Manifest manifest = jarFile.getManifest();
            return manifest == null ? null : manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        }
        finally {
            jarFile.close();
        }
    }

    @Nullable
    public static String detectSDKVersion(@NotNull final List<VirtualFile> jars) {
        for (final VirtualFile jar : jars) {
            if (jar.getName().equals(PathUtil.KOTLIN_COMPILER_JAR) && isKotlinCompilerJar(new File(jar.getPath()))) {
                final VirtualFile libDir = jar.getParent();
                if (libDir != null) {
                    final VirtualFile sdkHomeDir = libDir.getParent();
                    if (sdkHomeDir != null) {
                        return getSDKVersion(sdkHomeDir.getPath());
                    }
                }
            }
        }
        return null;
    }

    public static boolean isSDKConfiguredFor(@NotNull final Module module) {
        return getSDKHomeFor(module) != null;
    }

    @Nullable
    public static File getSDKHomeFor(@NotNull final Module module) {
        final GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);
        return findKotlinCompilerJar(FilenameIndex.getVirtualFilesByName(module.getProject(), PathUtil.KOTLIN_COMPILER_JAR, scope));
    }

    @Nullable
    private static File findKotlinCompilerJar(@NotNull final Collection<VirtualFile> jars) {
        for (final VirtualFile jar : jars) {
            final File file = new File(jar.getPath());
            if (isKotlinCompilerJar(file)) {
                return file;
            }
        }
        return null;
    }

    private static boolean isKotlinCompilerJar(@Nullable final File jar) {
        try {
            return jar != null && doIsKotlinCompilerJar(jar);
        }
        catch (final IOException e) {
            return false;
        }
    }

    private static boolean doIsKotlinCompilerJar(@NotNull final File jar) throws IOException {
        final JarFile jarFile = new JarFile(jar);
        try {
            for (final String entryName : KOTLIN_COMPILER_JAR_ENTRY_NAMES) {
                if (jarFile.getJarEntry(entryName) != null) {
                    return true;
                }
            }
            return false;
        }
        finally {
            jarFile.close();
        }
    }

    @NotNull
    public static String getSDKName(@NotNull final String version) {
        return "Kotlin " + version;
    }

    @NotNull
    public static PersistentLibraryKind<KotlinSdkProperties> getKotlinSdkLibraryKind() {
        return KOTLIN_SDK_KIND;
    }
}
