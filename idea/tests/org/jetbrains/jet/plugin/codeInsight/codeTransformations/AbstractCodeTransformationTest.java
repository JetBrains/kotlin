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

package org.jetbrains.jet.plugin.codeInsight.codeTransformations;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations.intentions.*;

import java.io.File;

public abstract class AbstractCodeTransformationTest extends LightCodeInsightTestCase {
    public void doTestFoldIfToAssignment(@NotNull String path) throws Exception {
        doTest(path, new FoldIfToAssignmentIntention());
    }

    public void doTestFoldIfToReturn(@NotNull String path) throws Exception {
        doTest(path, new FoldIfToReturnIntention());
    }

    public void doTestFoldIfToReturnAsymmetrically(@NotNull String path) throws Exception {
        doTest(path, new FoldIfToReturnAsymmetricallyIntention());
    }

    public void doTestFoldWhenToAssignment(@NotNull String path) throws Exception {
        doTest(path, new FoldWhenToAssignmentIntention());
    }

    public void doTestFoldWhenToReturn(@NotNull String path) throws Exception {
        doTest(path, new FoldWhenToReturnIntention());
    }

    public void doTestUnfoldAssignmentToIf(@NotNull String path) throws Exception {
        doTest(path, new UnfoldAssignmentToIfIntention());
    }

    public void doTestUnfoldAssignmentToWhen(@NotNull String path) throws Exception {
        doTest(path, new UnfoldAssignmentToWhenIntention());
    }

    public void doTestUnfoldReturnToIf(@NotNull String path) throws Exception {
        doTest(path, new UnfoldReturnToIfIntention());
    }

    public void doTestUnfoldReturnToWhen(@NotNull String path) throws Exception {
        doTest(path, new UnfoldAssignmentToIfIntention());
    }

    public void doTestRemoveUnnecessaryParentheses(@NotNull String path) throws Exception {
        doTest(path, new RemoveUnnecessaryParenthesesIntention());
    }

    private void doTest(@NotNull String path, @NotNull IntentionAction intentionAction) throws Exception {
        configureByFile(path);

        String fileText = FileUtil.loadFile(new File(path));
        String isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// IS_APPLICABLE: ");
        boolean isApplicableExpected = isApplicableString == null || isApplicableString.equals("true");

        assert isApplicableExpected == intentionAction.isAvailable(getProject(), getEditor(), getFile())
                : "isAvailable() for " + intentionAction.getClass() + " should return " + isApplicableExpected;
        if (isApplicableExpected) {
            invokeAndCheck(path, intentionAction);
        }
    }

    private void invokeAndCheck(@NotNull String path, @NotNull IntentionAction intentionAction) {
        intentionAction.invoke(getProject(), getEditor(), getFile());
        checkResultByFile(path + ".after");
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
