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

package org.jetbrains.jet.test.generator;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.utils.Printer;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class SimpleTestClassModel implements TestClassModel {
    private static final Comparator<TestEntityModel> BY_NAME = new Comparator<TestEntityModel>() {
        @Override
        public int compare(TestEntityModel o1, TestEntityModel o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
    private final File rootFile;
    private final boolean recursive;
    private final Pattern filenamePattern;
    private final String doTestMethodName;
    private final String testClassName;

    private Collection<TestClassModel> innerTestClasses;
    private Collection<TestMethodModel> testMethods;

    public SimpleTestClassModel(@NotNull File rootFile, boolean recursive, @NotNull Pattern filenamePattern, @NotNull String doTestMethodName) {
        this.rootFile = rootFile;
        this.recursive = recursive;
        this.filenamePattern = filenamePattern;
        this.doTestMethodName = doTestMethodName;
        this.testClassName = StringUtil.capitalize(TestGeneratorUtil.escapeForJavaIdentifier(rootFile.getName()));
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
                    if (file.isDirectory()) {
                        children.add(new SimpleTestClassModel(file, recursive, filenamePattern, doTestMethodName));
                    }
                }
            }
            Collections.sort(children, BY_NAME);
            innerTestClasses = children;
        }
        return innerTestClasses;
    }

    @NotNull
    @Override
    public Collection<TestMethodModel> getTestMethods() {
        if (testMethods == null) {
            if (!rootFile.isDirectory()) {
                testMethods = Collections.<TestMethodModel>singletonList(new SimpleTestMethodModel(rootFile, rootFile, doTestMethodName, filenamePattern));
            }
            else {
                List<TestMethodModel> result = Lists.newArrayList();

                result.add(new TestAllFilesPresentMethodModel());

                File[] listFiles = rootFile.listFiles();
                if (listFiles != null) {
                    for (File file : listFiles) {
                        if (!file.isDirectory() && filenamePattern.matcher(file.getName()).matches()) {
                            result.add(new SimpleTestMethodModel(rootFile, file, doTestMethodName, filenamePattern));
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
        public void generateBody(@NotNull Printer p, @NotNull String generatorClassFqName) {
            String assertTestsPresentStr =
                    String.format("JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), \"%s\", new File(\"%s\"), Pattern.compile(\"%s\"), %s);",
                                  generatorClassFqName, JetTestUtils.getFilePath(rootFile), StringUtil.escapeStringCharacters(filenamePattern.pattern()), recursive);
            p.println(assertTestsPresentStr);
        }

        @Override
        public String getDataString() {
            return null;
        }
    }
}
