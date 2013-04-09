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

package org.jetbrains.jet.resolve;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.List;

public abstract class ExtensibleResolveTestCase extends JetLiteFixture {
    private ExpectedResolveData expectedResolveData;

    @Override
    protected TestCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_AND_ANNOTATIONS);
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
        String text = loadFile(filePath);
        List<JetFile> files = JetTestUtils.createTestFiles("file.kt", text, new JetTestUtils.TestFileFactory<JetFile>() {
            @Override
            public JetFile create(String fileName, String text) {
                return expectedResolveData.createFileFromMarkedUpText(fileName, text);
            }
        });
        expectedResolveData.checkResult(expectedResolveData.analyze(files));
    }
}
