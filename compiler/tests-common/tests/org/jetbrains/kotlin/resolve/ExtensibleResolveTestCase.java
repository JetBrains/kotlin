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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;

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
        List<KtFile> files = KotlinTestUtils.createTestFiles("file.kt", text, new KotlinTestUtils.TestFileFactoryNoModules<KtFile>() {
            @NotNull
            @Override
            public KtFile create(@NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives) {
                return expectedResolveData.createFileFromMarkedUpText(fileName, text);
            }
        });
        expectedResolveData.checkResult(ExpectedResolveData.analyze(files, getEnvironment()));
    }
}
