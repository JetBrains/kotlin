/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.checkers;

import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.KotlinTestUtils;


public class TestCheckerUtil {
    @NotNull
    public static KtFile createCheckAndReturnPsiFile(@NotNull String fileName, @NotNull String text, @NotNull Project project) {
        KtFile myFile = KotlinTestUtils.createFile(fileName, text, project);
        ensureParsed(myFile);
        TestCase.assertEquals("light virtual file text mismatch", text, ((LightVirtualFile) myFile.getVirtualFile()).getContent().toString());
        TestCase.assertEquals("virtual file text mismatch", text, LoadTextUtil.loadText(myFile.getVirtualFile()));
        //noinspection ConstantConditions
        TestCase.assertEquals("doc text mismatch", text, myFile.getViewProvider().getDocument().getText());
        TestCase.assertEquals("psi text mismatch", text, myFile.getText());
        return myFile;
    }

    private static void ensureParsed(PsiFile file) {
        file.accept(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                element.acceptChildren(this);
            }
        });
    }
}
