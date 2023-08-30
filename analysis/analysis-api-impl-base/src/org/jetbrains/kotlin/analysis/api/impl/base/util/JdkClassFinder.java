/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util;

import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/*
 Copy of corresponding classes from IntelliJ source repository.
 We cannot reuse them directly as not all those methods are parts of intellij-core which is used in Analysis API
*/
class JdkClassFinder {
    // copy of 'com.intellij.openapi.projectRoots.impl.JavaSdkImpl.findClasses'
    static @NotNull List<String> findClasses(@NotNull Path jdkHome, boolean isJre) {
        List<String> result = new ArrayList<>();

        if (isExplodedModularRuntime(jdkHome)) {
            try {
                try (DirectoryStream<Path> roots = Files.newDirectoryStream(jdkHome.resolve("modules"))) {
                    for (Path root : roots) {
                        result.add(getUrlForLibraryRoot(root));
                    }
                }
            }
            catch (IOException ignore) {
            }
        }
        else if (isModularRuntime(jdkHome)) {
            String jrtBaseUrl = StandardFileSystems.JRT_PROTOCOL_PREFIX + vfsPath(jdkHome) + URLUtil.JAR_SEPARATOR;
            List<String> modules = readModulesFromReleaseFile(jdkHome);
            if (modules != null) {
                for (String module : modules) {
                    result.add(jrtBaseUrl + module);
                }
            }
            else {
                VirtualFile jrt = VirtualFileManager.getInstance().findFileByUrl(jrtBaseUrl);
                if (jrt != null) {
                    for (VirtualFile virtualFile : jrt.getChildren()) {
                        result.add(virtualFile.getUrl());
                    }
                }
            }
        }
        else {
            for (Path root : getJdkClassesRoots(jdkHome, isJre)) {
                result.add(getUrlForLibraryRoot(root));
            }
        }

        Collections.sort(result);
        return result;
    }

    // copy of 'com.intellij.openapi.projectRoots.impl.JavaSdkImpl.vfsPath'
    private static String vfsPath(@NotNull Path path) {
        return FileUtil.toSystemIndependentName(path.toAbsolutePath().toString());
    }


    // copy of 'com.intellij.openapi.projectRoots.JdkUtil.isExplodedModularRuntime'
    private static boolean isExplodedModularRuntime(@NotNull Path homePath) {
        return Files.isDirectory(homePath.resolve("modules/java.base"));
    }

    // copy of 'com.intellij.openapi.projectRoots.JdkUtil.isModularRuntime'
    private static boolean isModularRuntime(@NotNull Path homePath) {
        return Files.isRegularFile(homePath.resolve("lib/jrt-fs.jar")) || isExplodedModularRuntime(homePath);
    }

    // copy of 'com.intellij.openapi.vfs.VfsUtil.getUrlForLibraryRoot'
    private static @NotNull String getUrlForLibraryRoot(@NotNull Path libraryRoot) {
        return getUrlForLibraryRoot(libraryRoot.toAbsolutePath().toString(), libraryRoot.getFileName().toString());
    }

    // copy of `com.intellij.openapi.vfs.VfsUtil.getUrlForLibraryRoot`
    private static @NotNull String getUrlForLibraryRoot(@NotNull String libraryRootAbsolutePath, @NotNull String libraryRootFileName) {
        String path = FileUtil.toSystemIndependentName(libraryRootAbsolutePath);
        return FileTypeRegistry.getInstance().getFileTypeByFileName(libraryRootFileName) == ArchiveFileType.INSTANCE
               ? VirtualFileManager.constructUrl(StandardFileSystems.JAR_PROTOCOL, path + URLUtil.JAR_SEPARATOR)
               : VirtualFileManager.constructUrl(StandardFileSystems.FILE_PROTOCOL, path);
    }


    /**
     * Tries to load the list of modules in the JDK from the 'release' file. Returns null if the 'release' file is not there
     * or doesn't contain the expected information.
     * <p>
     * Copy of `com.intellij.openapi.projectRoots.impl.JavaSdkImpl.readModulesFromReleaseFile`
     */
    private static @Nullable List<String> readModulesFromReleaseFile(@NotNull Path jrtBaseDir) {
        try (InputStream stream = Files.newInputStream(jrtBaseDir.resolve("release"))) {
            Properties p = new Properties();
            p.load(stream);
            String modules = p.getProperty("MODULES");
            if (modules != null) {
                return StringUtil.split(StringUtil.unquoteString(modules), " ");
            }
        }
        catch (IOException | IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    // copy of `org.jetbrains.jps.model.java.impl.JavaSdkUtil.getJdkClassesRoots`
    private static @NotNull List<Path> getJdkClassesRoots(@NotNull Path home, boolean isJre) {
        Path[] jarDirs;
        Path fileName = home.getFileName();
        if (fileName != null && "Home".equals(fileName.toString()) && Files.exists(home.resolve("../Classes/classes.jar"))) {
            Path libDir = home.resolve("lib");
            Path classesDir = home.resolveSibling("Classes");
            Path libExtDir = libDir.resolve("ext");
            Path libEndorsedDir = libDir.resolve("endorsed");
            jarDirs = new Path[] {libEndorsedDir, libDir, classesDir, libExtDir};
        }
        else if (Files.exists(home.resolve("lib/jrt-fs.jar"))) {
            jarDirs = new Path[0];
        }
        else {
            Path libDir = home.resolve(isJre ? "lib" : "jre/lib");
            Path libExtDir = libDir.resolve("ext");
            Path libEndorsedDir = libDir.resolve("endorsed");
            jarDirs = new Path[] {libEndorsedDir, libDir, libExtDir};
        }

        List<Path> rootFiles = new ArrayList<>();

        if (Registry.is("project.structure.add.tools.jar.to.new.jdk", false)) {
            @SuppressWarnings("IdentifierGrammar") Path toolsJar = home.resolve("lib/tools.jar");
            if (Files.isRegularFile(toolsJar)) {
                rootFiles.add(toolsJar);
            }
        }

        Set<String> pathFilter = CollectionFactory.createFilePathSet();
        for (Path jarDir : jarDirs) {
            if (jarDir != null && Files.isDirectory(jarDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(jarDir, "*.jar")) {
                    for (Path jarFile : stream) {
                        String jarFileName = jarFile.getFileName().toString();
                        if (jarFileName.equals("alt-rt.jar") || jarFileName.equals("alt-string.jar")) {
                            continue;  // filter out alternative implementations
                        }
                        String canonicalPath = getCanonicalPath(jarFile);
                        if (canonicalPath == null || !pathFilter.add(canonicalPath)) {
                            continue;  // filter out duplicate (symbolically linked) .jar files commonly found in OS X JDK distributions
                        }
                        rootFiles.add(jarFile);
                    }
                }
                catch (IOException ignored) {
                }
            }
        }

        if (ContainerUtil.exists(rootFiles, path -> path.getFileName().toString().startsWith("ibm"))) {
            // ancient IBM JDKs split JRE classes between `rt.jar` and `vm.jar`, and the latter might be anywhere
            try (Stream<Path> paths = Files.walk(isJre ? home : home.resolve("jre"))) {
                paths.filter(path -> path.getFileName().toString().equals("vm.jar"))
                        .findFirst()
                        .ifPresent(rootFiles::add);
            }
            catch (IOException ignored) {
            }
        }

        Path classesZip = home.resolve("lib/classes.zip");
        if (Files.isRegularFile(classesZip)) {
            rootFiles.add(classesZip);
        }

        if (rootFiles.isEmpty()) {
            Path classesDir = home.resolve("classes");
            if (Files.isDirectory(classesDir)) {
                rootFiles.add(classesDir);
            }
        }

        return rootFiles;
    }

    // copy of `org.jetbrains.jps.model.java.impl.JavaSdkUtil.getCanonicalPath`
    private static @Nullable String getCanonicalPath(Path file) {
        try {
            return file.toRealPath().toString();
        }
        catch (IOException e) {
            return null;
        }
    }
}
