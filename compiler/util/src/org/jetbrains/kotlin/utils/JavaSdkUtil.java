/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils;

import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class JavaSdkUtil {
    public static @NotNull List<Path> getJdkClassesRoots(@NotNull Path home, boolean isJre) {
        Path[] jarDirs;
        Path fileName = home.getFileName();
        if (fileName != null && "Home".equals(fileName.toString()) && Files.exists(home.resolve("../Classes/classes.jar"))) {
            Path libDir = home.resolve("lib");
            Path classesDir = home.resolveSibling("Classes");
            Path libExtDir = libDir.resolve("ext");
            Path libEndorsedDir = libDir.resolve("endorsed");
            jarDirs = new Path[]{libEndorsedDir, libDir, classesDir, libExtDir};
        }
        else if (Files.exists(home.resolve("lib/jrt-fs.jar"))) {
            jarDirs = new Path[0];
        }
        else {
            Path libDir = home.resolve(isJre ? "lib" : "jre/lib");
            Path libExtDir = libDir.resolve("ext");
            Path libEndorsedDir = libDir.resolve("endorsed");
            jarDirs = new Path[]{libEndorsedDir, libDir, libExtDir};
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
                catch (IOException ignored) { }
            }
        }

        if (ContainerUtil.exists(rootFiles, path -> path.getFileName().toString().startsWith("ibm"))) {
            // ancient IBM JDKs split JRE classes between `rt.jar` and `vm.jar`, and the latter might be anywhere
            try (Stream<Path> paths = Files.walk(isJre ? home : home.resolve("jre"))) {
                paths.filter(path -> path.getFileName().toString().equals("vm.jar"))
                        .findFirst()
                        .ifPresent(rootFiles::add);
            }
            catch (IOException ignored) { }
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

    private static @Nullable String getCanonicalPath(Path file) {
        try {
            return file.toRealPath().toString();
        }
        catch (IOException e) {
            return null;
        }
    }
}
