/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.execution.configurations;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class PathEnvironmentVariableUtil {

    private static final String PATH = "PATH";

    private PathEnvironmentVariableUtil() { }

    /**
     * Finds an executable file with the specified base name, that is located in a directory
     * listed in PATH environment variable.
     *
     * @param fileBaseName file base name
     * @return {@link File} instance or null if not found
     */
    @Nullable
    public static File findInPath(@NotNull @NonNls String fileBaseName) {
        return findInPath(fileBaseName, null);
    }

    /**
     * Finds an executable file with the specified base name, that is located in a directory
     * listed in PATH environment variable and is accepted by filter.
     *
     * @param fileBaseName file base name
     * @param filter       exe file filter
     * @return {@link File} instance or null if not found
     */
    @Nullable
    public static File findInPath(@NotNull String fileBaseName, @Nullable FileFilter filter) {
        return findInPath(fileBaseName, getPathVariableValue(), filter);
    }

    /**
     * Finds an executable file with the specified base name, that is located in a directory
     * listed in the passed PATH environment variable value and is accepted by filter.
     *
     * @param fileBaseName      file base name
     * @param pathVariableValue value of PATH environment variable
     * @param filter            exe file filter
     * @return {@link File} instance or null if not found
     */
    @Nullable
    public static File findInPath(@NotNull String fileBaseName, @Nullable String pathVariableValue, @Nullable FileFilter filter) {
        List<File> exeFiles = findExeFilesInPath(true, filter, pathVariableValue, fileBaseName);
        return ContainerUtil.getFirstItem(exeFiles);
    }

    /**
     * Finds all executable files with the specified base name, that are located in directories
     * from PATH environment variable.
     *
     * @param fileBaseName file base name
     * @return file list
     */
    @NotNull
    public static List<File> findAllExeFilesInPath(@NotNull String fileBaseName) {
        return findAllExeFilesInPath(fileBaseName, null);
    }

    @NotNull
    public static List<File> findAllExeFilesInPath(@NotNull String fileBaseName, @Nullable FileFilter filter) {
        return findExeFilesInPath(false, filter, getPathVariableValue(), fileBaseName);
    }

    @NotNull
    private static List<File> findExeFilesInPath(boolean stopAfterFirstMatch,
            @Nullable FileFilter filter,
            @Nullable String pathEnvVarValue,
            String... fileBaseNames) {
        if (pathEnvVarValue == null) {
            return Collections.emptyList();
        }
        List<File> result = new SmartList<>();
        List<String> dirPaths = getPathDirs(pathEnvVarValue);
        for (String dirPath : dirPaths) {
            File dir = new File(dirPath);
            if (dir.isAbsolute() && dir.isDirectory()) {
                for (String fileBaseName : fileBaseNames) {
                    File exeFile = new File(dir, fileBaseName);
                    if (exeFile.isFile() && exeFile.canExecute()) {
                        if (filter == null || filter.accept(exeFile)) {
                            result.add(exeFile);
                            if (stopAfterFirstMatch) {
                                return result;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @NotNull
    public static List<String> getPathDirs(@NotNull String pathEnvVarValue) {
        return StringUtil.split(pathEnvVarValue, File.pathSeparator, true, true);
    }

    @NotNull
    public static List<String> getWindowsExecutableFileExtensions() {
        if (SystemInfo.isWindows) {
            String allExtensions = System.getenv("PATHEXT");
            if (allExtensions != null) {
                Collection<String> extensions = StringUtil.split(allExtensions, ";", true, true);
                extensions = ContainerUtil.filter(extensions, s -> !StringUtil.isEmpty(s) && s.startsWith("."));
                return ContainerUtil.map(extensions, StringUtil::toLowerCase);
            }
        }
        return Collections.emptyList();
    }

    @NotNull
    public static String findExecutableInWindowsPath(@NotNull String exePath) {
        return findExecutableInWindowsPath(exePath, exePath);
    }

    @Contract("_, !null -> !null")
    public static String findExecutableInWindowsPath(@NotNull String exePath, @Nullable String defaultPath) {
        if (SystemInfo.isWindows) {
            if (!StringUtil.containsChar(exePath, '/') && !StringUtil.containsChar(exePath, '\\')) {
                List<String> executableFileExtensions = getWindowsExecutableFileExtensions();

                String[] baseNames = ContainerUtil.map2Array(executableFileExtensions, String.class, s -> exePath + s);
                List<File> exeFiles = findExeFilesInPath(true, null, getPathVariableValue(), baseNames);
                File foundFile = ContainerUtil.getFirstItem(exeFiles);
                if (foundFile != null) {
                    return foundFile.getAbsolutePath();
                }
            }
        }
        return defaultPath;
    }

    /**
     * Retrieves the value of PATH environment variable
     */
    @Nullable
    public static String getPathVariableValue() {
        return EnvironmentUtil.getValue(PATH);
    }

    @Nullable
    public static File findExecutableInPathOnAnyOS(@NotNull @NonNls String fileBaseName) {
        if (SystemInfo.isWindows) {
            String[] fileNames = ContainerUtil.map2Array(getWindowsExecutableFileExtensions(), String.class,
                                                         (String extension) -> fileBaseName + extension);
            List<File> exeFiles = findExeFilesInPath(true, null, getPathVariableValue(), fileNames);
            return ContainerUtil.getFirstItem(exeFiles);
        }
        else {
            return findInPath(fileBaseName);
        }
    }
}
