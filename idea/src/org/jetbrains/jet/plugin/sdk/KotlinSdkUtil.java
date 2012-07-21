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

import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
    @NotNull private static final String KOTLIN_COMPILER_JAR = "kotlin-compiler.jar";

    private KotlinSdkUtil() {}

    public static boolean isSDKHome(@Nullable final VirtualFile dir) {
        return dir != null && isSDKHome(new File(dir.getPath()));
    }

    private static boolean isSDKHome(@NotNull final File dir) {
        return dir.isDirectory() && new File(new File(dir, "lib"), KOTLIN_COMPILER_JAR).isFile();
    }

    @Nullable
    public static String getSDKVersion(@NotNull final String sdkHomePath) {
        try {
            return FileUtil.loadFile(new File(sdkHomePath, "build.txt")).trim();
        }
        catch (final IOException e) {
            return null;
        }
    }

    @Nullable
    public static String detectSDKVersion(@NotNull final List<VirtualFile> jars) {
        for (final VirtualFile jar : jars) {
            if (jar.getName().equals(KOTLIN_COMPILER_JAR)) {
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

    @NotNull
    public static String getSDKName(@NotNull final String version) {
        return "Kotlin " + version;
    }

    @NotNull
    public static PersistentLibraryKind<KotlinSdkProperties> getKotlinSdkLibraryKind() {
        return KOTLIN_SDK_KIND;
    }
}
