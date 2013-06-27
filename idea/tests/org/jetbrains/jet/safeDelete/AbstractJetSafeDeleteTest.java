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

package org.jetbrains.jet.safeDelete;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.safeDelete.SafeDeleteHandler;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetObjectDeclarationName;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractJetSafeDeleteTest extends LightCodeInsightTestCase {
    public void doClassTest(@NotNull String path) throws Exception {
        doTest(path, JetClass.class);
    }

    public void doObjectTest(@NotNull String path) throws Exception {
        doTest(path, JetObjectDeclarationName.class);
    }

    private <T extends JetElement> void doTest(@NotNull String path, @NotNull Class<T> elementClass) throws Exception {
        configureByFile(path);

        DataContext dataContext = getCurrentEditorDataContext();
        PsiElement element = PsiTreeUtil.getParentOfType(LangDataKeys.PSI_ELEMENT.getData(dataContext), elementClass, false);

        try {
            new SafeDeleteHandler().invoke(getProject(), new PsiElement[] {element}, dataContext);
            checkResultByFile(path + ".after");
        }
        catch (BaseRefactoringProcessor.ConflictsInTestsException e) {
            List<String> messages = new ArrayList<String>(e.getMessages());
            Collections.sort(messages);

            File messageFile = new File(path + ".messages");
            String expectedMessage = FileUtil.loadFile(messageFile, CharsetToolkit.UTF8, true);
            assertEquals(expectedMessage, StringUtil.join(messages, "\n"));
        }
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
