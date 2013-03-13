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

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.util.Function;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.testing.LocalFileSystemUtils;

import java.io.File;
import java.util.Collections;

public class RenameInKotlinTest extends MultiFileTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LocalFileSystemUtils.refreshPath(getTestDataPath() + getTestRoot());
        LocalFileSystemUtils.refreshPath(getTestDataPath() + getTestRoot() + getTestName(true) + File.separator + "before");
        LocalFileSystemUtils.refreshPath(getTestDataPath() + getTestRoot() + getTestName(true) + File.separator + "after");
    }

    public void testRenameKotlinClass() throws Exception {
        doTestWithRenameClass(new FqName("testing.rename.First"), "Third");
    }

    public void testRenameKotlinMethod() throws Exception {
        doTestWithRenameMethod(new FqName("testing.rename.C"), "first", "second");
    }

    public void testRenameKotlinClassConstructor() throws Exception {
        doTestWithRenameClass(new FqName("X"), "TestX");
    }

    private void doTestWithRenameMethod(final FqName qClassName, final String oldMethodName, String newMethodName) throws Exception {
        doTestWithRename(new Function<PsiFile, PsiElement>() {
            @Override
            public PsiElement fun(PsiFile file) {
                BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) file)
                        .getBindingContext();
                ClassDescriptor classDescriptor = bindingContext.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, qClassName);

                assertNotNull(classDescriptor);
                JetScope scope = classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList());
                FunctionDescriptor methodDescriptor = scope.getFunctions(Name.identifier(oldMethodName)).iterator().next();
                return BindingContextUtils.callableDescriptorToDeclaration(bindingContext, methodDescriptor);
            }
        }, newMethodName);
    }

    private void doTestWithRenameClass(@NonNls final FqName qClassName, @NonNls String newName) throws Exception {
        doTestWithRename(new Function<PsiFile, PsiElement>() {
            @Override
            public PsiElement fun(PsiFile file) {
                BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) file)
                        .getBindingContext();
                ClassDescriptor classDescriptor = bindingContext.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, qClassName);

                assertNotNull(classDescriptor);

                return BindingContextUtils.classDescriptorToDeclaration(bindingContext, classDescriptor);
            }
        }, newName);
    }

    private void doTestWithRename(@NonNls final Function<PsiFile, PsiElement> elementToRenameCallback, @NonNls final String newName) throws Exception {
        doTest(new PerformAction() {
            @Override
            public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
                VirtualFile child = rootDir.findChild(getTestName(false) + ".kt");
                if (child == null) {
                    return;
                }

                Document document = FileDocumentManager.getInstance().getDocument(child);
                if (document == null) {
                    return;
                }

                PsiFile file = PsiDocumentManager.getInstance(getProject()).getPsiFile(document);
                if (!(file instanceof JetFile)) {
                    return;
                }

                PsiElement psiElement = elementToRenameCallback.fun(file);
                assertNotNull(psiElement);

                new RenameProcessor(myProject, psiElement, newName, true, true).run();
                PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                FileDocumentManager.getInstance().saveAllDocuments();
                VirtualFileManager.getInstance().refresh(false);
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
