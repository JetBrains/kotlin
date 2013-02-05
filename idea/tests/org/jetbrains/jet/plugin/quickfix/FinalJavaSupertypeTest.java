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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.testing.LocalFileSystemUtils;

import java.util.Arrays;
import java.util.List;

public class FinalJavaSupertypeTest extends JetQuickFixMultiFileTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        LocalFileSystemUtils.refreshPath(getTestDataPath());
    }

    public void testFinalJavaSupertype() throws Exception {
        String path = getTestDataPath() + "/../javaCode/";
        final VirtualFile rootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, path, myFilesToDelete, false);
        addSourceContentToRoots(myModule, rootDir);
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        doTest();
    }

    @Override
    protected String getCheckFileName() {
        throw new IllegalStateException("This test is to check that quickfix is not available, so no check file is needed.");
    }

    @Override
    protected List<String> getTestFileNames() {
        return Arrays.asList(getTestName(false) + ".before.kt");
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBuilder.getHomeDirectory() + "/idea/testData/quickfix/modifiers/finalSupertype/" + getTestName(true) + "/test/";
    }
}
