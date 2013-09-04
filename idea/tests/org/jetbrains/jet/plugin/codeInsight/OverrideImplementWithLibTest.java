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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.testing.ConfigLibraryUtil;

import java.io.File;

public final class OverrideImplementWithLibTest extends AbstractOverrideImplementTest {

    private static final String TEST_PATH = PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/overrideImplement/withLib";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(TEST_PATH);
    }

    public void doTestWithLib() {
        File dependency = new File(TEST_PATH + "/" + getTestName(true) + ".jar");
        assert dependency.exists();
        NewLibraryEditor editor = new NewLibraryEditor();
        editor.setName("dependency");
        editor.addRoot(VfsUtil.getUrlForLibraryRoot(dependency), OrderRootType.CLASSES);

        try {
            ConfigLibraryUtil.configureLibrary(myModule, getProjectDescriptor().getSdk(), editor);
            CodeInsightTestFixtureImpl.ensureIndexesUpToDate(getProject());
            doOverrideFileTest();
        }
        finally {
            ConfigLibraryUtil.unConfigureLibrary(myModule, getProjectDescriptor().getSdk(), editor.getName());
        }
    }

    public void testFakeOverride() throws Exception {
        doTestWithLib();
    }
}
