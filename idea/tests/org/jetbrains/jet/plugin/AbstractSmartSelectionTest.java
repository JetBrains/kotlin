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

package org.jetbrains.jet.plugin;

import com.intellij.codeInsight.editorActions.JoinLinesHandler;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions.*;
import org.jetbrains.jet.plugin.intentions.declarations.ConvertMemberToExtension;
import org.jetbrains.jet.plugin.intentions.declarations.SplitPropertyDeclarationIntention;
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester;
import org.jetbrains.jet.plugin.refactoring.JetNameValidator;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractSmartSelectionTest extends LightCodeInsightTestCase {

    public void doTestSmartSelection(@NotNull String path) throws Exception {
        configureByFile(path);
        String expectedResultText = JetTestUtils.getLastCommentInFile((JetFile) getFile());

        List<JetExpression> expressions = JetRefactoringUtil.getSmartSelectSuggestions(getEditor(), getFile(),
                                                                                       getEditor().getCaretModel().getOffset());
        List<String> textualExpressions = new ArrayList<String>();
        for (JetExpression expression : expressions) {
            textualExpressions.add(expression.getText());
        }
        assertEquals(expectedResultText, StringUtil.join(textualExpressions, "\n"));
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
