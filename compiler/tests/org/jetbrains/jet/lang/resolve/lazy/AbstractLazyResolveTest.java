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

package org.jetbrains.jet.lang.resolve.lazy;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import junit.framework.Assert;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.resolve.ExpectedResolveData;
import org.jetbrains.jet.resolve.JetExpectedResolveDataUtil;

import java.io.File;
import java.util.List;
import java.util.Map;

public abstract class AbstractLazyResolveTest extends JetLiteFixture {
    private ExpectedResolveData expectedResolveData;

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        expectedResolveData = getExpectedResolveData();
    }

    protected ExpectedResolveData getExpectedResolveData() {
        Project project = getProject();

        return new ExpectedResolveData(
                JetExpectedResolveDataUtil.prepareDefaultNameToDescriptors(project),
                JetExpectedResolveDataUtil.prepareDefaultNameToDeclaration(project),
                getEnvironment()) {
            @Override
            protected JetFile createJetFile(String fileName, String text) {
                return createCheckAndReturnPsiFile(fileName, null, text);
            }
        };
    }

    protected void doTest(@NonNls String testFile) throws Exception {
        String text = FileUtil.loadFile(new File(testFile), true);

        List<JetFile> files = JetTestUtils.createTestFiles("file.kt", text, new JetTestUtils.TestFileFactory<JetFile>() {
            @Override
            public JetFile create(String fileName, String text, Map<String, String> directives) {
                return expectedResolveData.createFileFromMarkedUpText(fileName, text);
            }
        });

        KotlinCodeAnalyzer resolveSession = LazyResolveTestUtil.resolveLazilyWithSession(files, getEnvironment(), true);

        PackageViewDescriptor actual = resolveSession.getModuleDescriptor().getPackage(new FqName("test"));
        Assert.assertNotNull("Package 'test' was not found", actual);

        resolveSession.forceResolveAll();

        expectedResolveData.checkResult(resolveSession.getBindingContext());
    }

}
