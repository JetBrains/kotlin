/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.generators.util.CoroutinesKt;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.utils.Printer;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SimpleTestClassModel extends TestClassModel {
    private static final Comparator<TestEntityModel> BY_NAME = Comparator.comparing(TestEntityModel::getName);

    @NotNull
    private final File rootFile;
    private final boolean recursive;
    private final boolean excludeParentDirs;
    @NotNull
    private final Pattern filenamePattern;
    @Nullable
    private final Pattern excludePattern;
    @Nullable
    private final Boolean checkFilenameStartsLowerCase;
    @NotNull
    private final String doTestMethodName;
    @NotNull
    private final String testClassName;
    private final Integer deep;
    @NotNull
    private final TargetBackend targetBackend;
    @NotNull
    private final Set<String> excludeDirs;
    @Nullable
    private Collection<TestClassModel> innerTestClasses;
    @Nullable
    private Collection<MethodModel> testMethods;

    @NotNull
    private final Collection<AnnotationModel> annotations;

    private final boolean skipIgnored;
    private final String testRunnerMethodName;
    private final List<String> additionalRunnerArguments;

    private boolean skipTestsForExperimentalCoroutines;

    public SimpleTestClassModel(
            @NotNull File rootFile,
            boolean recursive,
            boolean excludeParentDirs,
            @NotNull Pattern filenamePattern,
            @Nullable Pattern excludedPattern,
            @Nullable Boolean checkFilenameStartsLowerCase,
            @NotNull String doTestMethodName,
            @NotNull String testClassName,
            @NotNull TargetBackend targetBackend,
            @NotNull Collection<String> excludeDirs,
            boolean skipIgnored,
            String testRunnerMethodName,
            List<String> additionalRunnerArguments,
            Integer deep,
            @NotNull Collection<AnnotationModel> annotations,
            boolean skipTestsForExperimentalCoroutines
    ) {
        this.rootFile = rootFile;
        this.recursive = recursive;
        this.excludeParentDirs = excludeParentDirs;
        this.filenamePattern = filenamePattern;
        this.excludePattern = excludedPattern;
        this.doTestMethodName = doTestMethodName;
        this.testClassName = testClassName;
        this.targetBackend = targetBackend;
        this.checkFilenameStartsLowerCase = checkFilenameStartsLowerCase;
        this.excludeDirs = excludeDirs.isEmpty() ? Collections.emptySet() : new LinkedHashSet<>(excludeDirs);
        this.skipIgnored = skipIgnored;
        this.testRunnerMethodName = testRunnerMethodName;
        this.additionalRunnerArguments = additionalRunnerArguments;
        this.deep = deep;
        this.annotations = annotations;
        this.skipTestsForExperimentalCoroutines = skipTestsForExperimentalCoroutines;
    }

    @NotNull
    @Override
    public Collection<TestClassModel> getInnerTestClasses() {
        if (!rootFile.isDirectory() || !recursive || deep != null && deep < 1) {
            return Collections.emptyList();
        }

        if (innerTestClasses == null) {
            List<TestClassModel> children = new ArrayList<>();
            File[] files = rootFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && dirHasFilesInside(file) && !excludeDirs.contains(file.getName())) {
                        String innerTestClassName = TestGeneratorUtil.fileNameToJavaIdentifier(file);
                        children.add(new SimpleTestClassModel(
                                file, true, excludeParentDirs, filenamePattern, excludePattern, checkFilenameStartsLowerCase,
                                doTestMethodName, innerTestClassName, targetBackend, excludesStripOneDirectory(file.getName()),
                                skipIgnored, testRunnerMethodName, additionalRunnerArguments, deep != null ? deep - 1 : null, annotations,
                                skipTestsForExperimentalCoroutines)
                        );

                    }
                }
            }
            children.sort(BY_NAME);
            innerTestClasses = children;
        }
        return innerTestClasses;
    }

    @NotNull
    private Set<String> excludesStripOneDirectory(@NotNull String directoryName) {
        if (excludeDirs.isEmpty()) return excludeDirs;

        Set<String> result = new LinkedHashSet<>();
        for (String excludeDir : excludeDirs) {
            int firstSlash = excludeDir.indexOf('/');
            if (firstSlash >= 0 && excludeDir.substring(0, firstSlash).equals(directoryName)) {
                result.add(excludeDir.substring(firstSlash + 1));
            }
        }

        return result;
    }

    private static boolean dirHasFilesInside(@NotNull File dir) {
        return !FileUtil.processFilesRecursively(dir, File::isDirectory);
    }

    private static boolean dirHasSubDirs(@NotNull File dir) {
        File[] listFiles = dir.listFiles();
        if (listFiles == null) {
            return false;
        }
        for (File file : listFiles) {
            if (file.isDirectory()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public Collection<MethodModel> getMethods() {
        if (testMethods == null) {
            if (!rootFile.isDirectory()) {
                if (CoroutinesKt.isCommonCoroutineTest(rootFile)) {
                    testMethods = CoroutinesKt.createCommonCoroutinesTestMethodModels(rootFile, rootFile, filenamePattern,
                                                                                      checkFilenameStartsLowerCase, targetBackend,
                                                                                      skipIgnored, skipTestsForExperimentalCoroutines);
                }
                else {
                    testMethods = Collections.singletonList(new SimpleTestMethodModel(
                            rootFile, rootFile, filenamePattern, checkFilenameStartsLowerCase, targetBackend, skipIgnored
                    ));
                }
            }
            else {
                List<MethodModel> result = new ArrayList<>();

                result.add(new RunTestMethodModel(targetBackend, doTestMethodName, testRunnerMethodName, additionalRunnerArguments));

                result.add(new TestAllFilesPresentMethodModel());

                File[] listFiles = rootFile.listFiles();

                boolean hasCoroutines = false;

                if (listFiles != null && (deep == null || deep == 0)) {
                    for (File file : listFiles) {
                        boolean excluded = excludePattern != null && excludePattern.matcher(file.getName()).matches();
                        if (filenamePattern.matcher(file.getName()).matches() && !excluded) {

                            if (file.isDirectory() && excludeParentDirs && dirHasSubDirs(file)) {
                                continue;
                            }

                            if (!file.isDirectory() && CoroutinesKt.isCommonCoroutineTest(file)) {
                                hasCoroutines = true;
                                result.addAll(CoroutinesKt.createCommonCoroutinesTestMethodModels(rootFile, file,
                                                                                                  filenamePattern,
                                                                                                  checkFilenameStartsLowerCase,
                                                                                                  targetBackend, skipIgnored,
                                                                                                  skipTestsForExperimentalCoroutines));
                            }
                            else {
                                result.add(new SimpleTestMethodModel(rootFile, file, filenamePattern,
                                                                     checkFilenameStartsLowerCase, targetBackend, skipIgnored));
                            }
                        }
                    }
                }

                if (hasCoroutines) {
                    String methodName = doTestMethodName + "WithCoroutinesPackageReplacement";
                    result.add(new RunTestMethodWithPackageReplacementModel(targetBackend, methodName, testRunnerMethodName, additionalRunnerArguments));
                }

                result.sort(BY_NAME);

                testMethods = result;
            }
        }
        return testMethods;
    }

    @Override
    public boolean isEmpty() {
        boolean noTestMethods = getMethods().size() == 1;
        return noTestMethods && getInnerTestClasses().isEmpty();
    }

    @Override
    public String getDataString() {
        return KotlinTestUtils.getFilePath(rootFile);
    }

    @Nullable
    @Override
    public String getDataPathRoot() {
        return "$PROJECT_ROOT";
    }

    @NotNull
    @Override
    public String getName() {
        return testClassName;
    }

    @NotNull
    @Override
    public Collection<AnnotationModel> getAnnotations() {
        return annotations;
    }

    private class TestAllFilesPresentMethodModel extends TestMethodModel {
        @NotNull
        @Override
        public String getName() {
            return "testAllFilesPresentIn" + testClassName;
        }

        @Override
        public void generateBody(@NotNull Printer p) {
            StringBuilder exclude = new StringBuilder();
            for (String dir : excludeDirs) {
                exclude.append(", \"");
                exclude.append(StringUtil.escapeStringCharacters(dir));
                exclude.append("\"");
            }

            String excludedArgument;
            if (excludePattern != null) {
                excludedArgument = String.format("Pattern.compile(\"%s\")", StringUtil.escapeStringCharacters(excludePattern.pattern()));
            } else {
                excludedArgument = null;
            }

            String assertTestsPresentStr;

            if (targetBackend == TargetBackend.ANY) {
                assertTestsPresentStr = String.format(
                        "KotlinTestUtils.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File(\"%s\"), Pattern.compile(\"%s\"), %s, %s%s);",
                        KotlinTestUtils.getFilePath(rootFile), StringUtil.escapeStringCharacters(filenamePattern.pattern()), excludedArgument, recursive, exclude
                );
            } else {
                assertTestsPresentStr = String.format(
                        "KotlinTestUtils.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File(\"%s\"), Pattern.compile(\"%s\"), %s, %s.%s, %s%s);",
                        KotlinTestUtils.getFilePath(rootFile), StringUtil.escapeStringCharacters(filenamePattern.pattern()),
                        excludedArgument, TargetBackend.class.getSimpleName(), targetBackend.toString(), recursive, exclude
                );
            }

            p.println(assertTestsPresentStr);
        }

        @Override
        public String getDataString() {
            return null;
        }

        @Override
        public boolean shouldBeGenerated() {
            return true;
        }
    }
}
