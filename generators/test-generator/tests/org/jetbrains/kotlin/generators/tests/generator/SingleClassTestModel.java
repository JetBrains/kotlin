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
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.utils.Printer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class SingleClassTestModel implements TestClassModel {
    @NotNull
    private final File rootFile;
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
    @Nullable
    private Collection<MethodModel> methods;

    private final boolean skipIgnored;

    public SingleClassTestModel(
            @NotNull File rootFile,
            @NotNull Pattern filenamePattern,
            @Nullable Boolean checkFilenameStartsLowerCase,
            @NotNull String doTestMethodName,
            @NotNull String testClassName,
            @NotNull TargetBackend targetBackend,
            boolean skipIgnored
    ) {
        this.rootFile = rootFile;
        this.filenamePattern = filenamePattern;
        this.checkFilenameStartsLowerCase = checkFilenameStartsLowerCase;
        this.doTestMethodName = doTestMethodName;
        this.testClassName = testClassName;
        this.targetBackend = targetBackend;
        this.skipIgnored = skipIgnored;
    }

    @NotNull
    @Override
    public final Collection<TestClassModel> getInnerTestClasses() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<MethodModel> getMethods() {
        if (methods == null) {
            List<TestMethodModel> result = new ArrayList<>();

            result.add(new TestAllFilesPresentMethodModel());

            FileUtil.processFilesRecursively(rootFile, file -> {
                if (!file.isDirectory() && filenamePattern.matcher(file.getName()).matches()) {
                    result.addAll(getTestMethodsFromFile(file));
                }

                return true;
            });

            methods = CollectionsKt.sortedWith(result, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        }

        return methods;
    }

    @NotNull
    private Collection<TestMethodModel> getTestMethodsFromFile(File file) {
        return Collections.singletonList(new SimpleTestMethodModel(
                rootFile, file, doTestMethodName, filenamePattern, checkFilenameStartsLowerCase, targetBackend, skipIgnored
        ));
    }

    @Override
    public boolean isEmpty() {
        // There's always one test for checking if all tests are present
        return getMethods().size() <= 1;
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
            String assertTestsPresentStr = String.format(
                    "KotlinTestUtils.assertAllTestsPresentInSingleGeneratedClass(this.getClass(), new File(\"%s\"), Pattern.compile(\"%s\"), %s.%s);",
                    KotlinTestUtils.getFilePath(rootFile), StringUtil.escapeStringCharacters(filenamePattern.pattern()),
                    TargetBackend.class.getSimpleName(), targetBackend.toString()
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
