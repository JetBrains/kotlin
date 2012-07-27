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
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.util.PluginPathUtil;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author Maxim.Manuylov
 *         Date: 19.05.12
 */
public class KotlinSdkUtil {
    @NotNull private static PersistentLibraryKind<KotlinSdkProperties> KOTLIN_SDK_KIND =
            new PersistentLibraryKind<KotlinSdkProperties>("KotlinSDK", false) {
                @NotNull
                @Override
                public KotlinSdkProperties createDefaultProperties() {
                    return new KotlinSdkProperties(new File(""), "");
                }
            };
    @NotNull private static String[] KOTLIN_COMPILER_JAR_ENTRY_NAMES = {
        "org/jetbrains/jet/cli/KotlinCompiler.class",
        "org/jetbrains/jet/cli/jvm/K2JVMCompiler.class"
    };

    private KotlinSdkUtil() {}

    public static boolean isSDKHome(@Nullable VirtualFile dir) {
        return dir != null && isSDKHome(new File(dir.getPath()));
    }

    private static boolean isSDKHome(@NotNull File dir) {
        return dir.isDirectory() && isKotlinCompilerJar(PathUtil.getCompilerPath(dir));
    }

    @Nullable
    public static String getSDKVersion(@NotNull File sdkHome) {
        String buildNumber = getSDKBuildNumber(sdkHome);
        if (buildNumber == null) return null;
        int lastDotPos = buildNumber.lastIndexOf('.');
        return lastDotPos == -1 ? buildNumber : buildNumber.substring(0, lastDotPos);
    }

    @Nullable
    private static String getSDKBuildNumber(@NotNull File sdkHome) {
        try {
            return FileUtil.loadFile(new File(sdkHome, "build.txt")).trim();
        }
        catch (IOException e) {
            try {
                File compilerJar = PathUtil.getCompilerPath(sdkHome);
                return compilerJar == null ? null : getJarImplementationVersion(compilerJar);
            }
            catch (IOException e1) {
                return null;
            }
        }
    }

    @Nullable
    private static String getJarImplementationVersion(@NotNull File jar) throws IOException {
        JarFile jarFile = new JarFile(jar);
        try {
            Manifest manifest = jarFile.getManifest();
            return manifest == null ? null : manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        }
        finally {
            jarFile.close();
        }
    }

    @Nullable
    public static File detectSDKHome(@NotNull List<VirtualFile> jars) {
        for (VirtualFile jar : jars) {
            jar = prepare(jar);
            if (jar == null) continue;
            File file = new File(jar.getPath());
            if (file.getName().equals(PathUtil.KOTLIN_COMPILER_JAR) && isKotlinCompilerJar(file)) {
                File sdkHome = PathUtil.getSDKHomeByCompilerPath(file);
                if (sdkHome != null) {
                    return sdkHome;
                }
            }
        }
        return null;
    }

    @Nullable
    private static VirtualFile prepare(@NotNull VirtualFile jar) {
        if (jar.getFileSystem() instanceof JarFileSystem) {
            return JarFileSystem.getInstance().getVirtualFileForJar(jar);
        }
        return jar;
    }

    public static boolean isSDKConfiguredFor(@NotNull Module module) {
        return getSDKHomeFor(module) != null;
    }

    @Nullable
    public static File getSDKHomeFor(@NotNull Module module) {
        return findSDKHome(module, new HashSet<String>(), false);
    }

    @Nullable
    public static File findSDKHome(@NotNull Module module, @NotNull Set<String> checkedModuleNames, boolean isDependency) {
        checkedModuleNames.add(module.getName());
        for (OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (orderEntry instanceof ModuleOrderEntry) {
                ModuleOrderEntry moduleOrderEntry = (ModuleOrderEntry)orderEntry;
                Module depModule = moduleOrderEntry.getModule();
                if (depModule != null && !checkedModuleNames.contains(depModule.getName()) && isAvailable(moduleOrderEntry, isDependency)) {
                    File sdkHome = findSDKHome(depModule, checkedModuleNames, true);
                    if (sdkHome != null) {
                        return sdkHome;
                    }
                }
            }
            else if (orderEntry instanceof LibraryOrderEntry) {
                LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
                if (isAvailable(libraryOrderEntry, isDependency)) {
                    File sdkHome = detectSDKHome(Arrays.asList(libraryOrderEntry.getRootFiles(OrderRootType.CLASSES)));
                    if (sdkHome != null) {
                        return sdkHome;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isAvailable(@NotNull ExportableOrderEntry orderEntry, boolean isDependency) {
        return !isDependency || orderEntry.isExported();
    }

    private static boolean isKotlinCompilerJar(@Nullable File jar) {
        try {
            return jar != null && doIsKotlinCompilerJar(jar);
        }
        catch (IOException e) {
            return false;
        }
    }

    private static boolean doIsKotlinCompilerJar(@NotNull File jar) throws IOException {
        JarFile jarFile = new JarFile(jar);
        try {
            for (String entryName : KOTLIN_COMPILER_JAR_ENTRY_NAMES) {
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

    public static boolean isBundledSDK(@NotNull File sdkHome) {
        return sdkHome.equals(PluginPathUtil.getBundledSDKHome());
    }

    @NotNull
    public static String getSDKName(@NotNull File sdkHome, @NotNull String version) {
        return "Kotlin " + version + (isBundledSDK(sdkHome) ? " (bundled)" : "");
    }

    @NotNull
    public static PersistentLibraryKind<KotlinSdkProperties> getKotlinSdkLibraryKind() {
        return KOTLIN_SDK_KIND;
    }
}
