/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.*;
import org.jetbrains.kotlin.test.util.KtTestUtil;

import java.io.File;
import java.util.List;

public abstract class ExtensibleResolveTestCase extends KotlinTestWithEnvironment {
    private ExpectedResolveData expectedResolveData;

    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        CompilerConfiguration configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK);
        configuration.put(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING, true);
        return KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
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

    protected void doTest(@NonNls String filePath) {
        File file = new File(filePath);
        String text = KtTestUtil.doLoadFile(file);
        List<KtFile> files = TestFiles.createTestFiles("file.kt", text, new TestFiles.TestFileFactoryNoModules<KtFile>() {
            @NotNull
            @Override
            public KtFile create(@NotNull String fileName, @NotNull String text, @NotNull Directives directives) {
                return expectedResolveData.createFileFromMarkedUpText(fileName, text);
            }
        });
        expectedResolveData.checkResult(ExpectedResolveData.analyze(files, getEnvironment()));
    }
}
