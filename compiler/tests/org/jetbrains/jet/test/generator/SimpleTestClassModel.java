/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
* @author abreslav
*/
public class SimpleTestClassModel implements TestClassModel {
    private final File rootFile;
    private final boolean recursive;
    private final String extension;
    private final String doTestMethodName;
    private final String testClassName;

    public SimpleTestClassModel(@NotNull File rootFile, boolean recursive, @NotNull String extension, @NotNull String doTestMethodName) {
        this.rootFile = rootFile;
        this.recursive = recursive;
        this.extension = extension;
        this.doTestMethodName = doTestMethodName;
        this.testClassName = StringUtil.capitalize(rootFile.getName());
    }

    @Override
    public Collection<TestClassModel> getInnerTestClasses() {
        if (!rootFile.isDirectory() || !recursive) {
            return Collections.emptyList();
        }
        List<TestClassModel> children = Lists.newArrayList();
        File[] files = rootFile.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    children.add(new SimpleTestClassModel(file, recursive, extension, doTestMethodName));
                }
            }
        }
        return children;
    }

    @Override
    public Collection<TestMethodModel> getTestMethods() {
        if (!rootFile.isDirectory()) {
            return Collections.<TestMethodModel>singletonList(new SimpleTestMethodModel(rootFile, rootFile, doTestMethodName));
        }
        List<TestMethodModel> result = Lists.newArrayList();

        result.add(new TestMethodModel() {
            @Override
            public String getName() {
                return "testAllFilesPresentIn" + StringUtil.capitalize(rootFile.getName());
            }

            @Override
            public void generateBody(@NotNull Printer p, @NotNull String generatorClassFqName) {
                p.println("JetTestUtils.allTestsPresent(" +
                                "this.getClass(), " +
                                "\"", generatorClassFqName, "\", " +
                                "new File(\"", JetTestUtils.getFilePath(rootFile) + "\"), \"",
                                extension,
                                "\", ", recursive,
                          ");");
            }
        });

        File[] listFiles = rootFile.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                if (!file.isDirectory() && file.getName().endsWith("." + extension)) {
                    result.add(new SimpleTestMethodModel(rootFile, file, doTestMethodName));
                }
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return testClassName;
    }
}
