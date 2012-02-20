/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.refactoring.rename.RenameProcessor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;

/**
 * @author Nikolay Krasko
 */
public class RenameInKotlinTest extends MultiFileTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        String path = getTestDataPath() + getTestRoot();
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        if (virtualFile != null) {
            virtualFile.getChildren();
            virtualFile.refresh(false, true);
        }
    }

    public void testRenameKotlinClass() throws Exception {
        doTestWithRenameClass("testing.first.First", "Third");
    }

    private void doTestWithRenameClass(@NonNls final String qClassName, @NonNls final String newName) throws Exception {
        doTest(new PerformAction() {
            @Override
            public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
                BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCache(getProject(), GlobalSearchScope.allScope(getProject()));
                ClassDescriptor classDescriptor = bindingContext.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, qClassName);

                assertNotNull(classDescriptor);

                PsiElement psiElement = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, classDescriptor);

                assertNotNull(psiElement);

                new RenameProcessor(myProject, psiElement, newName, true, true).run();
                PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        });
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
