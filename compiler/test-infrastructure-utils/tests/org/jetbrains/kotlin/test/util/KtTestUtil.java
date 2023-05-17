/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import kotlin.collections.SetsKt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.KtAssert;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.test.InTextDirectivesUtils.isCompatibleTarget;

@SuppressWarnings({"WeakerAccess", "unused"})
public class KtTestUtil {
    private static final String homeDir = computeHomeDirectory();

    @NotNull
    public static File tmpDirForTest(@NotNull String testClassName, @NotNull String testName) throws IOException {
        return normalizeFile(FileUtil.createTempDirectory(testClassName, testName, false));
    }

    @NotNull
    public static File tmpDir(String name) throws IOException {
        return normalizeFile(FileUtil.createTempDirectory(name, "", false));
    }

    @NotNull
    public static File tmpDir(@NotNull File parentDir, @NotNull String name) throws IOException {
        return normalizeFile(FileUtil.createTempDirectory(parentDir, name, "", false));
    }

    @NotNull
    public static File tmpDirForReusableFolder(String name) throws IOException {
        return normalizeFile(FileUtil.createTempDirectory(new File(System.getProperty("java.io.tmpdir")), name, "", true));
    }

    private static File normalizeFile(File file) throws IOException {
        // Get canonical file to be sure that it's the same as inside the compiler,
        // for example, on Windows, if a canonical path contains any space from FileUtil.createTempDirectory we will get
        // a File with short names (8.3) in its path, and it will break some normalization passes in tests.
        return file.getCanonicalFile();
    }

    @NotNull
    public static KtFile createFile(@NotNull @NonNls String name, @NotNull String text, @NotNull Project project) {
        String shortName = name.substring(name.lastIndexOf('/') + 1);
        shortName = shortName.substring(shortName.lastIndexOf('\\') + 1);
        LightVirtualFile virtualFile = new LightVirtualFile(shortName, KotlinLanguage.INSTANCE, StringUtilRt.convertLineSeparators(text));

        virtualFile.setCharset(StandardCharsets.UTF_8);
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
             * Unfortunately, the FileNotFoundException will only show the relative path in its exception message.
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
    private static File getJdkHome(@NotNull String mainProperty) {
        return getJdkHome(mainProperty, null);
    }

    @NotNull
    private static File getJdkHome(@NotNull String mainProperty, @Nullable String propertyVariant) {
        return getJdkHome(mainProperty, propertyVariant, null);
    }

    @NotNull
    private static File getJdkHome(
            @NotNull String mainProperty,
            @Nullable String propertyVariant1,
            @Nullable String propertyVariant2
    ) {
        String jdkPath = getStringProperty(mainProperty);
        if (jdkPath == null && propertyVariant1 != null) {
            jdkPath = getStringProperty(propertyVariant1);
        }
        if (jdkPath == null && propertyVariant2 != null) {
            jdkPath = getStringProperty(propertyVariant2);
        }
        if (jdkPath == null) {
            throw new AssertionError("Environment variable " + mainProperty + " is not set!");
        }

        return new File(jdkPath);
    }

    private static String getStringProperty(@NotNull String propertyName) {
        String value = System.getProperty(propertyName);
        if (value != null) {
            return value;
        }
        return System.getenv(propertyName);
    }

    @NotNull
    public static File getJdk8Home() {
        return getJdkHome("JDK_1_8", "JDK_8", "JDK_18");
    }

    @NotNull
    public static File getJdk11Home() {
        return getJdkHome("JDK_11_0", "JDK_11");
    }

    @NotNull
    public static File getJdk17Home() {
        return getJdkHome("JDK_17_0", "JDK_17");
    }

    public static File getJdk21Home() {
        return getJdkHome("JDK_21_0", "JDK_21");
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

    // ---------------------- assert testdata presented by metadata ----------------------

    private static final String PLEASE_REGENERATE_TESTS = "Please regenerate tests (GenerateTests.kt)";

    public static void assertAllTestsPresentByMetadataWithExcluded(
            @NotNull Class<?> testCaseClass,
            @NotNull File testDataDir,
            @NotNull Pattern filenamePattern,
            @Nullable Pattern excludedPattern,
            boolean recursive,
            @NotNull String... excludeDirs
    ) {
        assertAllTestsPresentByMetadataWithExcluded(testCaseClass, testDataDir, filenamePattern, excludedPattern, TargetBackend.ANY, recursive, excludeDirs);
    }

    public static void assertAllTestsPresentByMetadata(
            @NotNull Class<?> testCaseClass,
            @NotNull File testDataDir,
            @NotNull Pattern filenamePattern,
            boolean recursive,
            @NotNull String... excludeDirs
    ) {
        assertAllTestsPresentByMetadata(
                testCaseClass,
                testDataDir,
                filenamePattern,
                TargetBackend.ANY,
                recursive,
                excludeDirs
        );
    }

    public static void assertAllTestsPresentByMetadataWithExcluded(
            @NotNull Class<?> testCaseClass,
            @NotNull File testDataDir,
            @NotNull Pattern filenamePattern,
            @Nullable Pattern excludedPattern,
            @NotNull TargetBackend targetBackend,
            boolean recursive,
            @NotNull String... excludeDirs
    ) {
        File rootFile = new File(getTestsRoot(testCaseClass));

        Set<String> filePaths = collectPathsMetadata(testCaseClass);
        Set<String> exclude = SetsKt.setOf(excludeDirs);

        File[] files = testDataDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (recursive && containsTestData(file, filenamePattern, excludedPattern) && !exclude.contains(file.getName())) {
                        assertTestClassPresentByMetadata(testCaseClass, file);
                    }
                }
                else {
                    boolean excluded = excludedPattern != null && excludedPattern.matcher(file.getName()).matches();
                    if (!excluded && filenamePattern.matcher(file.getName()).matches() && isCompatibleTarget(targetBackend, file)) {
                        assertFilePathPresent(file, rootFile, filePaths);
                    }
                }
            }
        }
    }

    public static void assertAllTestsPresentByMetadata(
            @NotNull Class<?> testCaseClass,
            @NotNull File testDataDir,
            @NotNull Pattern filenamePattern,
            @NotNull TargetBackend targetBackend,
            boolean recursive,
            @NotNull String... excludeDirs
    ) {
        assertAllTestsPresentByMetadataWithExcluded(testCaseClass, testDataDir, filenamePattern, null, targetBackend, recursive, excludeDirs);
    }

    public static void assertAllTestsPresentInSingleGeneratedClass(
            @NotNull Class<?> testCaseClass,
            @NotNull File testDataDir,
            @NotNull Pattern filenamePattern
    ) {
        assertAllTestsPresentInSingleGeneratedClass(testCaseClass, testDataDir, filenamePattern, TargetBackend.ANY);
    }

    public static void assertAllTestsPresentInSingleGeneratedClassWithExcluded(
            @NotNull Class<?> testCaseClass,
            @NotNull File testDataDir,
            @NotNull Pattern filenamePattern,
            @Nullable Pattern excludePattern
    ) {
        assertAllTestsPresentInSingleGeneratedClass(testCaseClass, testDataDir, filenamePattern, excludePattern, TargetBackend.ANY);
    }

    public static void assertAllTestsPresentInSingleGeneratedClass(
            @NotNull Class<?> testCaseClass,
            @NotNull File testDataDir,
            @NotNull Pattern filenamePattern,
            @NotNull TargetBackend targetBackend
    ) {
        assertAllTestsPresentInSingleGeneratedClass(testCaseClass, testDataDir, filenamePattern, null, targetBackend);
    }

    public static void assertAllTestsPresentInSingleGeneratedClass(
            @NotNull Class<?> testCaseClass,
            @NotNull File testDataDir,
            @NotNull Pattern filenamePattern,
            @Nullable Pattern excludePattern,
            @NotNull TargetBackend targetBackend
    ) {
        File rootFile = new File(getTestsRoot(testCaseClass));

        Set<String> filePaths = collectPathsMetadata(testCaseClass);

        FileUtil.processFilesRecursively(testDataDir, file -> {
            boolean excluded = excludePattern != null && excludePattern.matcher(file.getName()).matches();
            if (file.isFile() && !excluded && filenamePattern.matcher(file.getName()).matches() && isCompatibleTarget(targetBackend, file)) {
                assertFilePathPresent(file, rootFile, filePaths);
            }

            return true;
        });
    }

    private static void assertFilePathPresent(File file, File rootFile, Set<String> filePaths) {
        String path = FileUtil.getRelativePath(rootFile, file);
        if (path != null) {
            String relativePath = nameToCompare(path);
            if (!filePaths.contains(relativePath)) {
                KtAssert.fail("Test data file missing from the generated test class: " + file + "\n" + PLEASE_REGENERATE_TESTS);
            }
        }
    }

    private static Set<String> collectPathsMetadata(Class<?> testCaseClass) {
        return new HashSet<>(ContainerUtil.map(collectMethodsMetadata(testCaseClass), KtTestUtil::nameToCompare));
    }

    @Nullable
    public static String getMethodMetadata(Method method) {
        TestMetadata testMetadata = method.getAnnotation(TestMetadata.class);
        return (testMetadata != null) ? testMetadata.value() : null;
    }

    private static Set<String> collectMethodsMetadata(Class<?> testCaseClass) {
        Set<String> filePaths = new HashSet<>();
        for (Method method : testCaseClass.getDeclaredMethods()) {
            String path = getMethodMetadata(method);
            if (path != null) {
                filePaths.add(path);
            }
        }
        return filePaths;
    }

    private static boolean containsTestData(File dir, Pattern filenamePattern, @Nullable Pattern excludedPattern) {
        File[] files = dir.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                if (containsTestData(file, filenamePattern, excludedPattern)) {
                    return true;
                }
            }
            else {
                boolean excluded = excludedPattern != null && excludedPattern.matcher(file.getName()).matches();
                if (! excluded && filenamePattern.matcher(file.getName()).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void assertTestClassPresentByMetadata(@NotNull Class<?> outerClass, @NotNull File testDataDir) {
        for (Class<?> nestedClass : outerClass.getDeclaredClasses()) {
            TestMetadata testMetadata = nestedClass.getAnnotation(TestMetadata.class);
            if (testMetadata != null && testMetadata.value().equals(getFilePath(testDataDir))) {
                return;
            }
        }
        KtAssert.fail("Test data directory missing from the generated test class: " + testDataDir + "\n" + PLEASE_REGENERATE_TESTS);
    }

    public static String getTestsRoot(@NotNull Class<?> testCaseClass) {
        TestMetadata testClassMetadata = testCaseClass.getAnnotation(TestMetadata.class);
        KtAssert.assertNotNull("No metadata for class: " + testCaseClass, testClassMetadata);
        return testClassMetadata.value();
    }

    public static String nameToCompare(@NotNull String name) {
        return (SystemInfo.isFileSystemCaseSensitive ? name : name.toLowerCase()).replace('\\', '/');
    }
}
