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

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.safeDelete.SafeDeleteHandler;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractJetSafeDeleteTest extends JetLightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String pathBase = PluginTestCaseBase.getTestDataPathBase();
        myFixture.setTestDataPath(pathBase.substring(0, pathBase.lastIndexOf("/idea/testData")));
    }

    public void doClassTest(@NotNull String path) throws Exception {
        doTest(path, JetClass.class, false);
    }

    public void doObjectTest(@NotNull String path) throws Exception {
        doTest(path, JetObjectDeclaration.class, false);
    }

    public void doFunctionTest(@NotNull String path) throws Exception {
        doTest(path, JetNamedFunction.class, false);
    }

    public void doFunctionTestWithJava(@NotNull String path) throws Exception {
        doTest(path, JetNamedFunction.class, true);
    }

    public void doJavaMethodTest(@NotNull String path) throws Exception {
        doTest(path, PsiMethod.class, true);
    }

    public void doPropertyTest(@NotNull String path) throws Exception {
        doTest(path, JetProperty.class, false);
    }

    public void doPropertyTestWithJava(@NotNull String path) throws Exception {
        doTest(path, JetProperty.class, true);
    }

    public void doJavaPropertyTest(@NotNull String path) throws Exception {
        doTest(path, PsiMethod.class, true);
    }

    public void doTypeParameterTest(@NotNull String path) throws Exception {
        doTest(path, JetTypeParameter.class, false);
    }

    public void doTypeParameterTestWithJava(@NotNull String path) throws Exception {
        doTest(path, JetTypeParameter.class, true);
    }

    public void doValueParameterTest(@NotNull String path) throws Exception {
        doTest(path, JetParameter.class, false);
    }

    public void doValueParameterTestWithJava(@NotNull String path) throws Exception {
        doTest(path, JetParameter.class, true);
    }

    private <T extends PsiElement> void doTest(
            @NotNull String path, @NotNull Class<T> elementClass, boolean withJava) throws Exception {
        String[] filePaths;
        if (withJava) {
            filePaths = new String[]{path, path.endsWith(".java") ? path.replace(".java", ".kt") : path.replace(".kt", ".java")};
        }
        else {
            filePaths = new String[]{path};
        }

        Editor[] editors = new Editor[filePaths.length];
        int i = 0;
        for (String filePath : filePaths) {
            myFixture.configureByFile(filePath);
            editors[i++] = myFixture.getEditor();
        }

        PsiElement elementAtCaret = null;
        for (Editor editor : editors) {
            elementAtCaret = TargetElementUtilBase.findTargetElement(
                    editor, TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED | TargetElementUtilBase.ELEMENT_NAME_ACCEPTED
            );
            if (elementAtCaret != null) break;
        }

        assertNotNull("Couldn't find element at caret position", elementAtCaret);

        T element = PsiTreeUtil.getParentOfType(elementAtCaret, elementClass, false);

        try {
            SafeDeleteHandler.invoke(getProject(), new PsiElement[] {element}, null, true, null);
            for (int j = 0; j < filePaths.length; j++) {
                assertSameLinesWithFile(new File(filePaths[j] + ".after").getAbsolutePath(), editors[j].getDocument().getText());
            }
        }
        catch (BaseRefactoringProcessor.ConflictsInTestsException e) {
            List<String> messages = new ArrayList<String>(e.getMessages());
            Collections.sort(messages);

            File messageFile = new File(path + ".messages");
            assertSameLinesWithFile(messageFile.getAbsolutePath(), StringUtil.join(messages, "\n"));
        }
    }
}
