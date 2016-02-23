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

package org.jetbrains.kotlin.test;

import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.lazy.KotlinTestWithEnvironment;
import org.junit.Assert;

public abstract class KotlinLiteFixture extends KotlinTestWithEnvironment {
    protected String getTestDataPath() {
        return KotlinTestUtils.getTestDataPathBase();
    }

    protected KtFile createPsiFile(@Nullable String testName, @Nullable String fileName, String text) {
        if (fileName == null) {
            Assert.assertNotNull(testName);
            fileName = testName + ".kt";
        }
        return KotlinTestUtils.createFile(fileName, text, getProject());
    }

    private static void ensureParsed(PsiFile file) {
        file.accept(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                element.acceptChildren(this);
            }
        });
    }

    protected KtFile createCheckAndReturnPsiFile(String testName, String fileName, String text) {
        KtFile myFile = createPsiFile(testName, fileName, text);
        ensureParsed(myFile);
        assertEquals("light virtual file text mismatch", text, ((LightVirtualFile) myFile.getVirtualFile()).getContent().toString());
        assertEquals("virtual file text mismatch", text, LoadTextUtil.loadText(myFile.getVirtualFile()));
        //noinspection ConstantConditions
        assertEquals("doc text mismatch", text, myFile.getViewProvider().getDocument().getText());
        assertEquals("psi text mismatch", text, myFile.getText());
        return myFile;
    }
}
