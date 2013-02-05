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

package org.jetbrains.jet.plugin.refactoring.rename;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.refactoring.rename.RenameProcessor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.testing.LocalFileSystemUtils;

public class RenameInJavaTest extends MultiFileTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        LocalFileSystemUtils.refreshPath(getTestDataPath() + getTestRoot());
    }

    public void testRenameJavaClass() throws Exception {
        doTest("testing.SomeClass", "NewName");
    }

    public void todotestRenameJavaClassSamePackage() throws Exception {
        doTest("testing.SomeClass", "NewName");
    }


    private void doTest(@NonNls final String qClassName, @NonNls final String newName) throws Exception {
        doTest(new PerformAction() {
            @Override
            public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
                RenameInJavaTest.this.performAction(qClassName, newName);
            }
        });
    }

    private void performAction(String qClassName, String newName) throws Exception {
        PsiClass aClass = myJavaFacade.findClass(qClassName, GlobalSearchScope.allScope(getProject()));
        assertNotNull("Class " + qClassName + " not found", aClass);

        new RenameProcessor(myProject, aClass, newName, true, true).run();
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        FileDocumentManager.getInstance().saveAllDocuments();
    }

    @Override
    protected String getTestRoot() {
        return "/refactoring/rename/";
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }
}
