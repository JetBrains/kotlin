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

package org.jetbrains.jet.plugin.refactoring.introduce.introduceVariable;

import com.intellij.ide.DataManager;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractKotlinFunctionHandler;

import java.io.File;

public abstract class AbstractJetExtractionTest extends LightCodeInsightFixtureTestCase {
    protected void doIntroduceVariableTest(@NotNull String path) {
        doTest(path, new KotlinIntroduceVariableHandler());
    }

    protected void doTest(@NotNull String path, @NotNull RefactoringActionHandler handler) {
        File mainFile = new File(path);

        myFixture.setTestDataPath(JetTestCaseBuilder.getHomeDirectory() + "/" + mainFile.getParent());

        JetFile file = (JetFile) myFixture.configureByFile(mainFile.getName());

        PsiElement lastChild = file.getLastChild();
        assert lastChild != null;

        String expectedResultText = null;
        if (lastChild.getNode().getElementType().equals(JetTokens.BLOCK_COMMENT)) {
            String lastChildText = lastChild.getText();
            expectedResultText = lastChildText.substring(2, lastChildText.length() - 2).trim();
        }
        else if (lastChild.getNode().getElementType().equals(JetTokens.EOL_COMMENT)) {
            expectedResultText = lastChild.getText().substring(2).trim();
        }
        assert expectedResultText != null;

        handler.invoke(
                getProject(), myFixture.getEditor(), file, DataManager.getInstance().getDataContext(myFixture.getEditor().getComponent())
        );

        int endOffset = file.getLastChild().getTextRange().getStartOffset();
        assertEquals(expectedResultText, file.getText().substring(0, endOffset).trim());
    }
}
