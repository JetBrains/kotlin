/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.CoroutineTestUtilKt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.test.InTextDirectivesUtils.isDirectiveDefined;

public class TestFiles {
    /**
     * Syntax:
     *
     * // MODULE: name(dependency1, dependency2, ...)
     *
     * // FILE: name
     *
     * Several files may follow one module
     */
    private static final String MODULE_DELIMITER = ",\\s*";

    private static final Pattern FILE_OR_MODULE_PATTERN = Pattern.compile(
            "(?://\\s*MODULE:\\s*([^()\\n]+)(?:\\(([^()]+(?:" + MODULE_DELIMITER + "[^()]+)*)\\))?\\s*(?:\\(([^()]+(?:" + MODULE_DELIMITER + "[^()]+)*)\\))?\\s*)?" +
            "//\\s*FILE:\\s*(.*)$", Pattern.MULTILINE);

    private static final Pattern LINE_SEPARATOR_PATTERN = Pattern.compile("\\r\\n|\\r|\\n");

    @NotNull
    public static <M, F> List<F> createTestFiles(@Nullable String testFileName, String expectedText, TestFileFactory<M, F> factory) {
        return createTestFiles(testFileName, expectedText, factory, false, "");
    }

    @NotNull
    public static <M, F> List<F> createTestFiles(@Nullable String testFileName, String expectedText, TestFileFactory<M, F> factory, String coroutinesPackage) {
        return createTestFiles(testFileName, expectedText, factory, false, coroutinesPackage);
    }

    @NotNull
    public static <M, F> List<F> createTestFiles(String testFileName, String expectedText, TestFileFactory<M, F> factory,
            boolean preserveLocations, String coroutinesPackage) {
        Map<String, String> directives = KotlinTestUtils.parseDirectives(expectedText);

        List<F> testFiles = Lists.newArrayList();
        Matcher matcher = FILE_OR_MODULE_PATTERN.matcher(expectedText);
        boolean hasModules = false;
        if (!matcher.find()) {
            assert testFileName != null : "testFileName should not be null if no FILE directive defined";
            // One file
            testFiles.add(factory.createFile(null, testFileName, expectedText, directives));
        }
        else {
            int processedChars = 0;
            M module = null;
            // Many files
            while (true) {
                String moduleName = matcher.group(1);
                String moduleDependencies = matcher.group(2);
                String moduleFriends = matcher.group(3);
                if (moduleName != null) {
                    moduleName = moduleName.trim();
                    hasModules = true;
                    module = factory.createModule(moduleName, parseModuleList(moduleDependencies), parseModuleList(moduleFriends));
                }

                String fileName = matcher.group(4);
                int start = processedChars;

                boolean nextFileExists = matcher.find();
                int end;
                if (nextFileExists) {
                    end = matcher.start();
                }
                else {
                    end = expectedText.length();
                }
                String fileText = preserveLocations ?
                                  substringKeepingLocations(expectedText, start, end) :
                                  expectedText.substring(start,end);
                processedChars = end;

                testFiles.add(factory.createFile(module, fileName, fileText, directives));

                if (!nextFileExists) break;
            }
            assert processedChars == expectedText.length() : "Characters skipped from " +
                                                             processedChars +
                                                             " to " +
                                                             (expectedText.length() - 1);
        }

        if (isDirectiveDefined(expectedText, "WITH_COROUTINES")) {
            M supportModule = hasModules ? factory.createModule("support", Collections.emptyList(), Collections.emptyList()) : null;

            boolean isReleaseCoroutines =
                    !coroutinesPackage.contains("experimental") &&
                    !isDirectiveDefined(expectedText, "!LANGUAGE: -ReleaseCoroutines");

            boolean checkStateMachine = isDirectiveDefined(expectedText, "CHECK_STATE_MACHINE");
            boolean checkTailCallOptimization = isDirectiveDefined(expectedText, "CHECK_TAIL_CALL_OPTIMIZATION");

            testFiles.add(
                    factory.createFile(
                            supportModule,
                            "CoroutineUtil.kt",
                            CoroutineTestUtilKt.createTextForHelpers(isReleaseCoroutines, checkStateMachine, checkTailCallOptimization),
                            directives
                    ));
        }

        return testFiles;
    }

    private static String substringKeepingLocations(String string, int start, int end) {
        Matcher matcher = LINE_SEPARATOR_PATTERN.matcher(string);
        StringBuilder prefix = new StringBuilder();
        int lastLineOffset = 0;
        while (matcher.find()) {
            if (matcher.end() > start) {
                break;
            }

            lastLineOffset = matcher.end();
            prefix.append('\n');
        }

        while (lastLineOffset++ < start) {
            prefix.append(' ');
        }

        return prefix + string.substring(start, end);
    }

    private static List<String> parseModuleList(@Nullable String dependencies) {
        if (dependencies == null) return Collections.emptyList();
        return kotlin.text.StringsKt.split(dependencies, Pattern.compile(MODULE_DELIMITER), 0);
    }

    public interface TestFileFactory<M, F> {
        F createFile(@Nullable M module, @NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives);
        M createModule(@NotNull String name, @NotNull List<String> dependencies, @NotNull List<String> friends);
    }

    public static abstract class TestFileFactoryNoModules<F> implements TestFileFactory<Void, F> {
        @Override
        public final F createFile(
                @Nullable Void module,
                @NotNull String fileName,
                @NotNull String text,
                @NotNull Map<String, String> directives
        ) {
            return create(fileName, text, directives);
        }

        @NotNull
        public abstract F create(@NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives);

        @Override
        public Void createModule(@NotNull String name, @NotNull List<String> dependencies, @NotNull List<String> friends) {
            return null;
        }
    }
}
