/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.test.TestFiles;

import java.io.File;
import java.util.List;
import java.util.Map;

public abstract class ExtensibleResolveTestCase extends KotlinTestWithEnvironment {
    private ExpectedResolveData expectedResolveData;

    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        expectedResolveData = getExpectedResolveData();
    }

    @Override
    protected void tearDown() throws Exception {
        expectedResolveData = null;
        super.tearDown();
    }

    protected abstract ExpectedResolveData getExpectedResolveData();

    protected void doTest(@NonNls String filePath) throws Exception {
        File file = new File(filePath);
        String text = KotlinTestUtils.doLoadFile(file);
        List<KtFile> files = TestFiles.createTestFiles("file.kt", text, new TestFiles.TestFileFactoryNoModules<KtFile>() {
            @NotNull
            @Override
            public KtFile create(@NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives) {
                return expectedResolveData.createFileFromMarkedUpText(fileName, text);
            }
        }, "");
        expectedResolveData.checkResult(ExpectedResolveData.analyze(files, getEnvironment()));
    }
}
