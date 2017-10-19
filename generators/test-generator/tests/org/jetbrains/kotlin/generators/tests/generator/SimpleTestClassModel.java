/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.generators.tests.generator;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.utils.Printer;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class SimpleTestClassModel implements TestClassModel {
    private static final Comparator<TestEntityModel> BY_NAME = Comparator.comparing(TestEntityModel::getName);

    @NotNull
    private final File rootFile;
    private final boolean recursive;
    private final boolean excludeParentDirs;
    @NotNull
    private final Pattern filenamePattern;
    @Nullable
    private final Boolean checkFilenameStartsLowerCase;
    @NotNull
    private final String doTestMethodName;
    @NotNull
    private final String testClassName;
    @NotNull
    private final TargetBackend targetBackend;
    @NotNull
    private final Set<String> excludeDirs;
    @Nullable
    private Collection<TestClassModel> innerTestClasses;
    @Nullable
    private Collection<MethodModel> testMethods;

    private final boolean skipIgnored;

    public SimpleTestClassModel(
            @NotNull File rootFile,
            boolean recursive,
            boolean excludeParentDirs,
            @NotNull Pattern filenamePattern,
            @Nullable Boolean checkFilenameStartsLowerCase,
            @NotNull String doTestMethodName,
            @NotNull String testClassName,
            @NotNull TargetBackend targetBackend,
            @NotNull Collection<String> excludeDirs,
            boolean skipIgnored
    ) {
        this.rootFile = rootFile;
        this.recursive = recursive;
        this.excludeParentDirs = excludeParentDirs;
        this.filenamePattern = filenamePattern;
        this.doTestMethodName = doTestMethodName;
        this.testClassName = testClassName;
        this.targetBackend = targetBackend;
        this.checkFilenameStartsLowerCase = checkFilenameStartsLowerCase;
        this.excludeDirs = excludeDirs.isEmpty() ? Collections.emptySet() : new LinkedHashSet<>(excludeDirs);
        this.skipIgnored = skipIgnored;
    }

    @NotNull
    @Override
    public Collection<TestClassModel> getInnerTestClasses() {
        if (!rootFile.isDirectory() || !recursive) {
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
                                             file, true, excludeParentDirs, filenamePattern, checkFilenameStartsLowerCase,
                                             doTestMethodName, innerTestClassName, targetBackend, excludesStripOneDirectory(file.getName()),
                                             skipIgnored)
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
                testMethods = Collections.singletonList(new SimpleTestMethodModel(
                        rootFile, rootFile, doTestMethodName, filenamePattern, checkFilenameStartsLowerCase, targetBackend, skipIgnored
                ));
            }
            else {
                List<MethodModel> result = new ArrayList<>();

                result.add(new TestAllFilesPresentMethodModel());

                File[] listFiles = rootFile.listFiles();
                if (listFiles != null) {
                    for (File file : listFiles) {
                        if (filenamePattern.matcher(file.getName()).matches()) {

                            if (file.isDirectory() && excludeParentDirs && dirHasSubDirs(file)) {
                                continue;
                            }

                            result.add(new SimpleTestMethodModel(rootFile, file, doTestMethodName, filenamePattern,
                                                                 checkFilenameStartsLowerCase, targetBackend, skipIgnored));
                        }
                    }
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

    private class TestAllFilesPresentMethodModel implements TestMethodModel {
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
            String assertTestsPresentStr = String.format(
                    "KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File(\"%s\"), Pattern.compile(\"%s\"), %s.%s, %s%s);",
                    KotlinTestUtils.getFilePath(rootFile), StringUtil.escapeStringCharacters(filenamePattern.pattern()), TargetBackend.class.getSimpleName(), targetBackend.toString(), recursive, exclude
            );
            p.println(assertTestsPresentStr);
        }

        @Override
        public String getDataString() {
            return null;
        }

        @Override
        public void generateSignature(@NotNull Printer p) {
            TestMethodModel.DefaultImpls.generateSignature(this, p);
        }

        @Override
        public boolean shouldBeGenerated() {
            return true;
        }
    }
}
