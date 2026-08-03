/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.lang.JavaVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.CompilerConfigurationCreationKt;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.util.CurrentJavaVersion.currentJavaVersion;

public class KotlinTestUtils {

    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("^//\\s*([A-Z_0-9]+)(:[ \\t]*(.*))?$", Pattern.MULTILINE);

    private KotlinTestUtils() {
    }

    @NotNull
    public static File tmpDirForTest(TestInfo testInfo) throws IOException {
        //noinspection OptionalGetWithoutIsPresent
        return KtTestUtil.tmpDirForTest(testInfo.getTestClass().get().getSimpleName(), testInfo.getDisplayName());
    }

    @NotNull
    public static CompilerConfiguration newConfiguration() {
        CompilerConfiguration configuration = CompilerConfigurationCreationKt.create(CompilerConfiguration.Companion);
        String TEST_MODULE_NAME = "test-module";
        configuration.put(CommonConfigurationKeys.MODULE_NAME, TEST_MODULE_NAME);
        configuration.put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, new MessageCollector() {
            @Override
            public void clear() {
            }

            @Override
            public void report(
                    @NotNull CompilerMessageSeverity severity, @NotNull String message, @Nullable CompilerMessageSourceLocation location
            ) {
                if (severity == CompilerMessageSeverity.ERROR) {
                    String prefix = location == null
                                  ? ""
                                  : "(" + location.getPath() + ":" + location.getLine() + ":" + location.getColumn() + ") ";
                    throw new AssertionError(prefix + message);
                }
            }

            @Override
            public boolean hasErrors() {
                return false;
            }
        });

        return configuration;
    }

    @NotNull
    public static CompilerConfiguration newConfiguration(
            @NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind,
            @NotNull File... extraClasspath
    ) {
        return newConfiguration(configurationKind, jdkKind, Arrays.asList(extraClasspath), Collections.emptyList());
    }

    @NotNull
    public static CompilerConfiguration newConfiguration(
            @NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind,
            @NotNull List<File> classpath,
            @NotNull List<File> javaSource
    ) {
        CompilerConfiguration configuration = newConfiguration();
        JvmContentRootsKt.addJavaSourceRoots(configuration, javaSource);
        if (jdkKind == TestJdkKind.MOCK_JDK) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, KtTestUtil.findMockJdkRtJar());
            configuration.put(JVMConfigurationKeys.NO_JDK, true);
        }
        else if (jdkKind == TestJdkKind.MODIFIED_MOCK_JDK) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, KtTestUtil.findMockJdkRtModified());
            configuration.put(JVMConfigurationKeys.NO_JDK, true);
        }
        else if (jdkKind == TestJdkKind.FULL_JDK_11) {
            configuration.put(JVMConfigurationKeys.JDK_HOME, KtTestUtil.getJdk11Home());
        }
        else if (jdkKind == TestJdkKind.FULL_JDK_17) {
            configuration.put(JVMConfigurationKeys.JDK_HOME, KtTestUtil.getJdk17Home());
        }
        else if (jdkKind == TestJdkKind.FULL_JDK_21) {
            configuration.put(JVMConfigurationKeys.JDK_HOME, KtTestUtil.getJdk21Home());
        }
        else if (jdkKind == TestJdkKind.FULL_JDK_25) {
            configuration.put(JVMConfigurationKeys.JDK_HOME, KtTestUtil.getJdk25Home());
        }
        else if (currentJavaVersion().compareTo(JavaVersion.compose(9)) >= 0) {
            configuration.put(JVMConfigurationKeys.JDK_HOME, new File(System.getProperty("java.home")));
        }

        if (configurationKind.getWithRuntime()) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.runtimeJarForTests());
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.scriptRuntimeJarForTests());
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.kotlinTestJarForTests());
        }
        else if (configurationKind.getWithMockRuntime()) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.minimalRuntimeJarForTests());
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.scriptRuntimeJarForTests());
        }
        if (configurationKind.getWithReflection()) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.reflectJarForTests());
        }

        JvmContentRootsKt.addJvmClasspathRoots(configuration, classpath);
        JvmContentRootsKt.configureJdkClasspathRoots(configuration);

        return configuration;
    }

    @NotNull
    public static Directives parseDirectives(String expectedText) {
        return parseDirectives(expectedText, new Directives());
    }

    @NotNull
    public static Directives parseDirectives(String expectedText, @NotNull Directives directives) {
        Matcher directiveMatcher = DIRECTIVE_PATTERN.matcher(expectedText);
        while (directiveMatcher.find()) {
            String name = directiveMatcher.group(1);
            String value = directiveMatcher.group(3);
            directives.put(name, value);
        }
        return directives;
    }

    @NotNull
    private static KtFile loadKtFile(@NotNull Project project, @NotNull File ioFile) throws IOException {
        String text = FileUtil.loadFile(ioFile, true);
        return new KtPsiFactory(project).createPhysicalFile(ioFile.getName(), text);
    }

    @NotNull
    public static List<KtFile> loadToKtFiles(@NotNull KotlinCoreEnvironment environment, @NotNull List<File> files) throws IOException {
        List<KtFile> ktFiles = Lists.newArrayList();
        for (File file : files) {
            ktFiles.add(loadKtFile(environment.getProject(), file));
        }
        return ktFiles;
    }
}
