/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.utils;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class PathUtil {

    public static final String JS_LIB_JAR_NAME = "kotlin-jslib.jar";
    public static final String JS_LIB_JS_NAME = "kotlinEcma3.js";
    public static final String JDK_ANNOTATIONS_JAR = "kotlin-jdk-annotations.jar";
    public static final String KOTLIN_JAVA_RUNTIME_JAR = "kotlin-runtime.jar";
    public static final String BUILD_VERSION_NAME = "build.txt";
    public static final String HOME_FOLDER_NAME = "kotlinc";

    private static final File NO_PATH = new File("<no_path>");

    public static final Function<String, File> KOTLIN_HOME_DIRECTORY_ADAPTER = new Function<String, File>() {
        private final Pattern homeDirPattern = Pattern.compile(HOME_FOLDER_NAME);
        private final Pattern buildFilePattern = Pattern.compile(BUILD_VERSION_NAME);

        @Override
        public File fun(String path) {
            if (path == null) {
                return null;
            }

            File directory = new File(path);
            if (!(directory.exists() || directory.isDirectory())) {
                return null;
            }

            if (checkIsHomeDirectory(directory)) {
                return directory;
            }

            List<File> homeSubfolders = KotlinVfsUtil.getFilesInDirectoryByPattern(directory, homeDirPattern);
            if (!homeSubfolders.isEmpty()) {
                assert homeSubfolders.size() == 1;
                File homeNamedDir = homeSubfolders.get(0);
                if (checkIsHomeDirectory(homeNamedDir)) {
                    return homeNamedDir;
                }
            }

            File parentDirectory = directory.getParentFile();
            if (parentDirectory != null && checkIsHomeDirectory(parentDirectory)) {
                return parentDirectory;
            }

            return null;
        }

        private boolean checkIsHomeDirectory(File dir) {
            return dir.getName().equals(HOME_FOLDER_NAME) && !KotlinVfsUtil.getFilesInDirectoryByPattern(dir, buildFilePattern).isEmpty();
        }
    };

    private PathUtil() {}

    @NotNull
    public static KotlinPaths getKotlinPathsForIdeaPlugin() {
        return new KotlinPathsFromHomeDir(getCompilerPathForIdeaPlugin());
    }

    @NotNull
    public static KotlinPaths getKotlinPathsForJpsPlugin() {
        return new KotlinPathsFromHomeDir(getCompilerPathForJpsPlugin());
    }

    @NotNull
    public static KotlinPaths getKotlinPathsForJpsPluginOrJpsTests() {
        if ("true".equalsIgnoreCase(System.getProperty("kotlin.jps.tests"))) {
            return getKotlinPathsForDistDirectory();
        }
        return getKotlinPathsForJpsPlugin();
    }

    @NotNull
    public static KotlinPaths getKotlinPathsForCompiler() {
        if (!getPathUtilJar().isFile()) {
            // Not running from a jar, i.e. it is it must be a unit test
            return getKotlinPathsForDistDirectory();
        }
        return new KotlinPathsFromHomeDir(getCompilerPathForCompilerJar());
    }

    @NotNull
    public static KotlinPaths getKotlinPathsForDistDirectory() {
        return new KotlinPathsFromHomeDir(new File("dist", HOME_FOLDER_NAME));
    }

    @NotNull
    public static KotlinPaths getKotlinStandaloneCompilerPaths(@NotNull String path) {
        File homePath = KOTLIN_HOME_DIRECTORY_ADAPTER.fun(path);
        if (homePath == null) {
            throw new IllegalArgumentException(String.format("Can't get home path from '%s'", path));
        }

        return new KotlinPathsFromHomeDir(homePath);
    }

    @NotNull
    private static File getCompilerPathForCompilerJar() {
        File jar = getPathUtilJar();

        if (!jar.exists()) return NO_PATH;

        if (jar.getName().equals("kotlin-compiler.jar")) {
            File lib = jar.getParentFile();
            return lib.getParentFile();
        }

        return NO_PATH;
    }

    @NotNull
    private static File getCompilerPathForJpsPlugin() {
        File jar = getPathUtilJar();

        if (!jar.exists()) return NO_PATH;

        if (jar.getName().equals("kotlin-jps-plugin.jar")) {
            File pluginHome = jar.getParentFile().getParentFile().getParentFile();
            return new File(pluginHome, HOME_FOLDER_NAME);
        }

        return NO_PATH;
    }

    @NotNull
    private static File getCompilerPathForIdeaPlugin() {
        File jar = getPathUtilJar();

        if (!jar.exists()) return NO_PATH;

        if (jar.getName().equals("kotlin-plugin.jar")) {
            File lib = jar.getParentFile();
            File pluginHome = lib.getParentFile();

            return new File(pluginHome, HOME_FOLDER_NAME);
        }

        return NO_PATH;
    }

    private static File getPathUtilJar() {
        return getJarPathForClass(PathUtil.class);
    }

    @NotNull
    public static File getJarPathForClass(@NotNull Class aClass) {
        String resourceRoot = PathManager.getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
        return new File(resourceRoot).getAbsoluteFile();
    }

    @NotNull
    public static VirtualFile jarFileOrDirectoryToVirtualFile(@NotNull File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                return VirtualFileManager.getInstance()
                        .findFileByUrl("file://" + FileUtil.toSystemIndependentName(file.getAbsolutePath()));
            }
            else {
                return VirtualFileManager.getInstance().findFileByUrl("jar://" + FileUtil.toSystemIndependentName(file.getAbsolutePath()) + "!/");
            }
        }
        else {
            throw new IllegalStateException("Path " + file + " does not exist.");
        }
    }

    @NotNull
    public static File findRtJar() {
        return findRtJar(System.getProperty("java.home"));
    }

    private static File findRtJar(String javaHome) {
        if (SystemInfo.isMac && !SystemInfo.isJavaVersionAtLeast("1.7")) {
            File classesJar = new File(new File(javaHome).getParentFile(), "Classes/classes.jar");
            if (classesJar.exists()) {
                return classesJar;
            }

            throw new IllegalArgumentException("No classes.jar found under " + classesJar.getParent());
        }
        else {
            File rtJar = new File(javaHome, "lib/rt.jar");
            if (rtJar.exists()) {
                return rtJar;
            }

            throw new IllegalArgumentException("No rt.jar found under " + rtJar.getParent());
        }
    }
}
