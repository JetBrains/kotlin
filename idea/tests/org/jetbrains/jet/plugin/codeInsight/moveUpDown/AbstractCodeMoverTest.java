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

package org.jetbrains.jet.plugin.codeInsight.moveUpDown;

import com.intellij.codeInsight.editorActions.moveUpDown.MoveStatementDownAction;
import com.intellij.codeInsight.editorActions.moveUpDown.MoveStatementUpAction;
import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.codeInsight.upDownMover.JetDeclarationMover;
import org.jetbrains.jet.plugin.codeInsight.upDownMover.JetExpressionMover;

import java.io.File;

public abstract class AbstractCodeMoverTest extends LightCodeInsightTestCase {
    public void doTestClassBodyDeclaration(@NotNull String path) throws Exception {
        doTest(path, JetDeclarationMover.class);
    }

    public void doTestExpression(@NotNull String path) throws Exception {
        doTest(path, JetExpressionMover.class);
    }

    private void doTest(@NotNull String path, @NotNull Class<? extends StatementUpDownMover> defaultMoverClass) throws Exception {
        configureByFile(path);

        String fileText = FileUtil.loadFile(new File(path), true);
        String direction = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// MOVE: ");

        boolean down = true;
        if ("up".equals(direction)) {
            down = false;
        }
        else if ("down".equals(direction)) {
            down = true;
        }
        else {
            fail("Direction is not specified");
        }

        String isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// IS_APPLICABLE: ");
        boolean isApplicableExpected = isApplicableString == null || isApplicableString.equals("true");

        StatementUpDownMover[] movers = Extensions.getExtensions(StatementUpDownMover.STATEMENT_UP_DOWN_MOVER_EP);
        StatementUpDownMover.MoveInfo info = new StatementUpDownMover.MoveInfo();
        StatementUpDownMover actualMover = null;
        for (StatementUpDownMover mover : movers) {
            if (mover.checkAvailable(getEditor(), getFile(), info, down)) {
                actualMover = mover;
                break;
            }
        }

        String moverClassName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// MOVER_CLASS: ");
        if (moverClassName == null) {
            moverClassName = defaultMoverClass.getName();
        }

        assertTrue("No mover found", actualMover != null);
        assertEquals("Unmatched movers", moverClassName, actualMover.getClass().getName());
        assertEquals("Invalid applicability", isApplicableExpected, info.toMove2 != null);

        if (isApplicableExpected) {
            invokeAndCheck(path, down);
        }
    }

    private void invokeAndCheck(@NotNull String path, final boolean down) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                EditorAction action = down ? new MoveStatementDownAction() : new MoveStatementUpAction();
                action.actionPerformed(getEditor(), getCurrentEditorDataContext());
            }
        });
        checkResultByFile(path + ".after");
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
