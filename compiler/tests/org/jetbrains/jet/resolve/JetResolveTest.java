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

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;

@SuppressWarnings("JUnitTestCaseWithNoTests")
public class JetResolveTest extends ExtensibleResolveTestCase {

    private final String path;
    private final String name;

    @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
    public JetResolveTest(String path, String name) {
        this.path = path;
        this.name = name;
    }

    @Override
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

    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/compiler/testData";
    }

    private static String getHomeDirectory() {
        String resourceRoot = PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class");
        assertNotNull(resourceRoot);

        return new File(resourceRoot).getParentFile().getParentFile().getParent();
    }

    @Override
    public String getName() {
        return "test" + name;
    }

    @Override
    protected void runTest() throws Throwable {
        doTest(path);
    }

    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(getHomeDirectory() + "/compiler/testData/", "/resolve/",
                                                    true, JetTestCaseBuilder.filterByExtension("resolve"),
                                                    new JetTestCaseBuilder.NamedTestFactory() {
                                                        @NotNull
                                                        @Override
                                                        public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                                                            return new JetResolveTest(dataPath + "/" + name + ".resolve", name);
                                                        }
                                                    });
    }
}
