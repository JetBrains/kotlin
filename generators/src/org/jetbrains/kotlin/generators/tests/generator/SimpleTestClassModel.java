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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.utils.Printer;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.generators.tests.generator.TestGenerator.TargetBackend;

public class SimpleTestClassModel implements TestClassModel {
    private static final Comparator<TestEntityModel> BY_NAME = new Comparator<TestEntityModel>() {
        @Override
        public int compare(@NotNull TestEntityModel o1, @NotNull TestEntityModel o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
    @NotNull
    private final File rootFile;
    private final boolean recursive;
    private final boolean excludeParentDirs;
    @NotNull
    private final Pattern filenamePattern;
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
    private Collection<TestMethodModel> testMethods;

    public SimpleTestClassModel(
            @NotNull File rootFile,
            boolean recursive,
            boolean excludeParentDirs,
            @NotNull Pattern filenamePattern,
            @NotNull String doTestMethodName,
            @NotNull String testClassName,
            @NotNull TargetBackend targetBackend,
            @NotNull Collection<String> excludeDirs
    ) {
        this.rootFile = rootFile;
        this.recursive = recursive;
        this.excludeParentDirs = excludeParentDirs;
        this.filenamePattern = filenamePattern;
        this.doTestMethodName = doTestMethodName;
        this.testClassName = testClassName;
        this.targetBackend = targetBackend;
        this.excludeDirs = excludeDirs.isEmpty() ? Collections.<String>emptySet() : new LinkedHashSet<String>(excludeDirs);
    }

    @NotNull
    @Override
    public Collection<TestClassModel> getInnerTestClasses() {
        if (!rootFile.isDirectory() || !recursive) {
            return Collections.emptyList();
        }

        if (innerTestClasses == null) {
            List<TestClassModel> children = Lists.newArrayList();
            File[] files = rootFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && dirHasFilesInside(file) && !excludeDirs.contains(file.getName())) {
                        String innerTestClassName = TestGeneratorUtil.fileNameToJavaIdentifier(file);
                        children.add(new SimpleTestClassModel(file, true, excludeParentDirs, filenamePattern, doTestMethodName,
                                                              innerTestClassName, targetBackend, excludesStripOneDirectory(file.getName())));
                    }
                }
            }
            Collections.sort(children, BY_NAME);
            innerTestClasses = children;
        }
        return innerTestClasses;
    }

    @NotNull
    private Set<String> excludesStripOneDirectory(@NotNull String directoryName) {
        if (excludeDirs.isEmpty()) return excludeDirs;

        Set<String> result = new LinkedHashSet<String>();
        for (String excludeDir : excludeDirs) {
            int firstSlash = excludeDir.indexOf('/');
            if (firstSlash >= 0 && excludeDir.substring(0, firstSlash).equals(directoryName)) {
                result.add(excludeDir.substring(firstSlash + 1));
            }
        }

        return result;
    }

    private static boolean dirHasFilesInside(@NotNull File dir) {
        return !FileUtil.processFilesRecursively(dir, new Processor<File>() {
            @Override
            public boolean process(File file) {
                return file.isDirectory();
            }
        });
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
    public Collection<TestMethodModel> getTestMethods() {
        if (testMethods == null) {
            if (!rootFile.isDirectory()) {
                testMethods = Collections.<TestMethodModel>singletonList(new SimpleTestMethodModel(rootFile, rootFile, doTestMethodName, filenamePattern,
                                                                                                   targetBackend));
            }
            else {
                List<TestMethodModel> result = Lists.newArrayList();

                result.add(new TestAllFilesPresentMethodModel());

                File[] listFiles = rootFile.listFiles();
                if (listFiles != null) {
                    for (File file : listFiles) {
                        if (filenamePattern.matcher(file.getName()).matches()) {

                            if (file.isDirectory() && excludeParentDirs && dirHasSubDirs(file)) {
                                continue;
                            }

                            result.add(new SimpleTestMethodModel(rootFile, file, doTestMethodName, filenamePattern, targetBackend));
                        }
                    }
                }
                Collections.sort(result, BY_NAME);

                testMethods = result;
            }
        }
        return testMethods;
    }

    @Override
    public boolean isEmpty() {
        return getTestMethods().size() == 1 && getInnerTestClasses().isEmpty();
    }

    @Override
    public String getDataString() {
        return JetTestUtils.getFilePath(rootFile);
    }

    @Nullable
    @Override
    public String getDataPathRoot() {
        return "$PROJECT_ROOT";
    }

    @Override
    public String getName() {
        return testClassName;
    }

    private class TestAllFilesPresentMethodModel implements TestMethodModel {
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
                    "JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File(\"%s\"), Pattern.compile(\"%s\"), %s%s);",
                    JetTestUtils.getFilePath(rootFile), StringUtil.escapeStringCharacters(filenamePattern.pattern()), recursive, exclude
            );
            p.println(assertTestsPresentStr);
        }

        @Override
        public String getDataString() {
            return null;
        }
    }
}
