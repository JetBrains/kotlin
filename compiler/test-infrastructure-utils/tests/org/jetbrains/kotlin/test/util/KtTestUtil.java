/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class KtTestUtil {
    private static String homeDir = computeHomeDirectory();

    @NotNull
    public static File tmpDirForTest(@NotNull String testClassName, @NotNull String testName) throws IOException {
        return normalizeFile(FileUtil.createTempDirectory(testClassName, testName, false));
    }

    @NotNull
    public static File tmpDir(String name) throws IOException {
        return normalizeFile(FileUtil.createTempDirectory(name, "", false));
    }

    @NotNull
    public static File tmpDirForReusableFolder(String name) throws IOException {
        return normalizeFile(FileUtil.createTempDirectory(new File(System.getProperty("java.io.tmpdir")), name, "", true));
    }

    private static File normalizeFile(File file) throws IOException {
        // Get canonical file to be sure that it's the same as inside the compiler,
        // for example, on Windows, if a canonical path contains any space from FileUtil.createTempDirectory we will get
        // a File with short names (8.3) in its path and it will break some normalization passes in tests.
        return file.getCanonicalFile();
    }

    @NotNull
    public static KtFile createFile(@NotNull @NonNls String name, @NotNull String text, @NotNull Project project) {
        String shortName = name.substring(name.lastIndexOf('/') + 1);
        shortName = shortName.substring(shortName.lastIndexOf('\\') + 1);
        LightVirtualFile virtualFile = new LightVirtualFile(shortName, KotlinLanguage.INSTANCE, StringUtilRt.convertLineSeparators(text));

        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        PsiFileFactoryImpl factory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(project);
        //noinspection ConstantConditions
        return (KtFile) factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false);
    }

    public static String doLoadFile(String myFullDataPath, String name) throws IOException {
        String fullName = myFullDataPath + File.separatorChar + name;
        return doLoadFile(new File(fullName));
    }

    public static String doLoadFile(@NotNull File file) throws IOException {
        try {
            return FileUtil.loadFile(file, CharsetToolkit.UTF8, true);
        }
        catch (FileNotFoundException fileNotFoundException) {
            /*
             * Unfortunately, the FileNotFoundException will only show the relative path in it's exception message.
             * This clarifies the exception by showing the full path.
             */
            String messageWithFullPath = file.getAbsolutePath() + " (No such file or directory)";
            throw new IOException(
                    "Ensure you have your 'Working Directory' configured correctly as the root " +
                    "Kotlin project directory in your test configuration\n\t" +
                    messageWithFullPath,
                    fileNotFoundException);
        }
    }

    public static String getFilePath(File file) {
        return FileUtil.toSystemIndependentName(file.getPath());
    }

    @NotNull
    public static File getJdk9Home() {
        String jdk9 = System.getenv("JDK_9");
        if (jdk9 == null) {
            jdk9 = System.getenv("JDK_19");
            if (jdk9 == null) {
                throw new AssertionError("Environment variable JDK_9 is not set!");
            }
        }
        return new File(jdk9);
    }

    @Nullable
    public static File getJdk11Home() {
        String jdk11 = System.getenv("JDK_11");
        if (jdk11 == null) {
            return null;
        }
        return new File(jdk11);
    }

    @NotNull
    public static File getJdk15Home() {
        String jdk15 = System.getenv("JDK_15");

        if (jdk15 == null) {
            jdk15 = System.getenv("JDK_15_0");
        }

        if (jdk15 == null) {
            throw new AssertionError("Environment variable JDK_15 is not set!");
        }
        return new File(jdk15);
    }

    @NotNull
    public static String getTestDataPathBase() {
        return getHomeDirectory() + "/compiler/testData";
    }

    @NotNull
    public static String getHomeDirectory() {
        return homeDir;
    }

    @NotNull
    private static String computeHomeDirectory() {
        String userDir = System.getProperty("user.dir");
        File dir = new File(userDir == null ? "." : userDir);
        return FileUtil.toCanonicalPath(dir.getAbsolutePath());
    }

    public static File findMockJdkRtJar() {
        return new File(getHomeDirectory(), "compiler/testData/mockJDK/jre/lib/rt.jar");
    }

    // Differs from common mock JDK only by one additional 'nonExistingMethod' in Collection and constructor from Double in Throwable
    // It's needed to test the way we load additional built-ins members that neither in black nor white lists
    public static File findMockJdkRtModified() {
        return new File(getHomeDirectory(), "compiler/testData/mockJDKModified/rt.jar");
    }

    public static File findAndroidApiJar() {
        String androidJarProp = System.getProperty("android.jar");
        File androidJarFile = androidJarProp == null ? null : new File(androidJarProp);
        if (androidJarFile == null || !androidJarFile.isFile()) {
            throw new RuntimeException(
                    "Unable to get a valid path from 'android.jar' property (" +
                    androidJarProp +
                    "), please point it to the 'android.jar' file location");
        }
        return androidJarFile;
    }

    @NotNull
    public static File findAndroidSdk() {
        String androidSdkProp = System.getProperty("android.sdk");
        File androidSdkDir = androidSdkProp == null ? null : new File(androidSdkProp);
        if (androidSdkDir == null || !androidSdkDir.isDirectory()) {
            throw new RuntimeException(
                    "Unable to get a valid path from 'android.sdk' property (" +
                    androidSdkProp +
                    "), please point it to the android SDK location");
        }
        return androidSdkDir;
    }

    public static String getAndroidSdkSystemIndependentPath() {
        return PathUtil.toSystemIndependentName(findAndroidSdk().getAbsolutePath());
    }

    public static File getAnnotationsJar() {
        return new File(getHomeDirectory(), "compiler/testData/mockJDK/jre/lib/annotations.jar");
    }

    public static void mkdirs(@NotNull File file) {
        if (file.isDirectory()) {
            return;
        }
        if (!file.mkdirs()) {
            if (file.exists()) {
                throw new IllegalStateException("Failed to create " + file + ": file exists and not a directory");
            }
            throw new IllegalStateException("Failed to create " + file);
        }
    }
}
