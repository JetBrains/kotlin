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

package org.jetbrains.jet.plugin.intentions;

import com.intellij.codeInsight.editorActions.JoinLinesHandler;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions.*;
import org.jetbrains.jet.plugin.intentions.declarations.ConvertMemberToExtension;
import org.jetbrains.jet.plugin.intentions.declarations.SplitPropertyDeclarationIntention;
import org.junit.Assert;

import java.io.File;

public abstract class AbstractCodeTransformationTest extends LightCodeInsightTestCase {
    public void doTestFoldIfToAssignment(@NotNull String path) throws Exception {
        doTestIntention(path, new FoldIfToAssignmentIntention());
    }

    public void doTestFoldIfToReturn(@NotNull String path) throws Exception {
        doTestIntention(path, new FoldIfToReturnIntention());
    }

    public void doTestFoldIfToReturnAsymmetrically(@NotNull String path) throws Exception {
        doTestIntention(path, new FoldIfToReturnAsymmetricallyIntention());
    }

    public void doTestFoldWhenToAssignment(@NotNull String path) throws Exception {
        doTestIntention(path, new FoldWhenToAssignmentIntention());
    }

    public void doTestFoldWhenToReturn(@NotNull String path) throws Exception {
        doTestIntention(path, new FoldWhenToReturnIntention());
    }

    public void doTestUnfoldAssignmentToIf(@NotNull String path) throws Exception {
        doTestIntention(path, new UnfoldAssignmentToIfIntention());
    }

    public void doTestUnfoldAssignmentToWhen(@NotNull String path) throws Exception {
        doTestIntention(path, new UnfoldAssignmentToWhenIntention());
    }

    public void doTestUnfoldPropertyToIf(@NotNull String path) throws Exception {
        doTestIntention(path, new UnfoldPropertyToIfIntention());
    }

    public void doTestUnfoldPropertyToWhen(@NotNull String path) throws Exception {
        doTestIntention(path, new UnfoldPropertyToWhenIntention());
    }

    public void doTestUnfoldReturnToIf(@NotNull String path) throws Exception {
        doTestIntention(path, new UnfoldReturnToIfIntention());
    }

    public void doTestUnfoldReturnToWhen(@NotNull String path) throws Exception {
        doTestIntention(path, new UnfoldReturnToWhenIntention());
    }

    public void doTestIfToWhen(@NotNull String path) throws Exception {
        doTestIntention(path, new IfToWhenIntention());
    }

    public void doTestWhenToIf(@NotNull String path) throws Exception {
        doTestIntention(path, new WhenToIfIntention());
    }

    public void doTestFlattenWhen(@NotNull String path) throws Exception {
        doTestIntention(path, new FlattenWhenIntention());
    }

    public void doTestMergeWhen(@NotNull String path) throws Exception {
        doTestIntention(path, new MergeWhenIntention());
    }

    public void doTestIntroduceWhenSubject(@NotNull String path) throws Exception {
        doTestIntention(path, new IntroduceWhenSubjectIntention());
    }

    public void doTestEliminateWhenSubject(@NotNull String path) throws Exception {
        doTestIntention(path, new EliminateWhenSubjectIntention());
    }

    public void doTestSplitProperty(@NotNull String path) throws Exception {
        doTestIntention(path, new SplitPropertyDeclarationIntention());
    }

    public void doTestJoinProperty(@NotNull String path) throws Exception {
        doTestAction(path, new JoinLinesHandler(null));
    }

    public void doTestRemoveUnnecessaryParentheses(@NotNull String path) throws Exception {
        doTestIntention(path, new RemoveUnnecessaryParenthesesIntention());
    }

    public void doTestRemoveCurlyFromTemplate(@NotNull String path) throws Exception {
        doTestIntention(path, new RemoveCurlyBracesFromTemplateIntention());
    }

    public void doTestInsertCurlyToTemplate(@NotNull String path) throws Exception {
        doTestIntention(path, new InsertCurlyBracesToTemplateIntention());
    }

    public void doTestMoveLambdaInsideParentheses(@NotNull String path) throws Exception {
        doTestIntention(path, new MoveLambdaInsideParenthesesIntention());
    }

    public void doTestMoveLambdaOutsideParentheses(@NotNull String path) throws Exception {
        doTestIntention(path, new MoveLambdaOutsideParenthesesIntention());
    }

    public void doTestConvertMemberToExtension(@NotNull String path) throws Exception {
        doTestIntention(path, new ConvertMemberToExtension());
    }

    public void doTestReconstructType(@NotNull String path) throws Exception {
        doTestIntention(path, new ReconstructTypeInCastOrIsAction());
    }

    public void doTestReplaceWithDotQualifiedMethodCall(@NotNull String path) throws Exception {
        doTestIntention(path, new ReplaceWithDotQualifiedMethodCallIntention());
    }

    public void doTestReplaceWithInfixFunctionCall(@NotNull String path) throws Exception {
        doTestIntention(path, new ReplaceWithInfixFunctionCallIntention());
    }

    public void doTestReplaceExplicitFunctionLiteralParamWithIt(@NotNull String path) throws Exception {
        doTestIntention(path, new ReplaceExplicitFunctionLiteralParamWithItIntention());
    }

    public void doTestReplaceItWithExplicitFunctionLiteralParam(@NotNull String path) throws Exception {
        doTestIntention(path, new ReplaceItWithExplicitFunctionLiteralParamIntention());
    }

    public void doTestRemoveBraces(@NotNull String path) throws Exception {
        doTestIntention(path, new RemoveBracesIntention());
    }

    public void doTestAddBraces(@NotNull String path) throws Exception {
        doTestIntention(path, new AddBracesIntention());
    }

    public void doTestReplaceGetIntention(@NotNull String path) throws Exception {
        doTestIntention(path, new TestableReplaceGetIntention());
    }

    public void doTestReplaceContainsIntention(@NotNull String path) throws Exception {
        doTestIntention(path, new TestableReplaceContainsIntention());
    }

    public void doTestReplaceBinaryInfixIntention(@NotNull String path) throws Exception {
        doTestIntention(path, new TestableReplaceBinaryInfixIntention());
    }

    public void doTestReplaceUnaryPrefixIntention(@NotNull String path) throws Exception {
        doTestIntention(path, new TestableReplaceUnaryPrefixIntention());
    }

    public void doTestReplaceInvokeIntention(@NotNull String path) throws Exception {
        doTestIntention(path, new TestableReplaceInvokeIntention());
    }

    private void doTestIntention(@NotNull String path, @NotNull IntentionAction intentionAction) throws Exception {
        configureByFile(path);

        String fileText = FileUtil.loadFile(new File(path), true);
        String isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// IS_APPLICABLE: ");
        boolean isApplicableExpected = isApplicableString == null || isApplicableString.equals("true");

        Assert.assertTrue(
                "isAvailable() for " + intentionAction.getClass() + " should return " + isApplicableExpected,
                isApplicableExpected == intentionAction.isAvailable(getProject(), getEditor(), getFile()));

        String intentionTextString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// INTENTION_TEXT: ");

        if (intentionTextString != null) {
            assertEquals("Intention text mismatch.", intentionTextString, intentionAction.getText());
        }

        String shouldFailString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// SHOULD_FAIL_WITH: ");

        try {
            if (isApplicableExpected) {
                intentionAction.invoke(getProject(), getEditor(), getFile());
                // Don't bother checking if it should have failed.
                if (shouldFailString == null) {
                    checkResultByFile(path + ".after");
                }
            }
            assertNull("Expected test to fail.", shouldFailString);
        } catch (IntentionTestException e) {
            assertEquals("Failure message mismatch.", shouldFailString, e.getMessage());
        }
    }

    private void doTestAction(@NotNull String path, @NotNull EditorActionHandler actionHandler) throws Exception {
        configureByFile(path);
        actionHandler.execute(getEditor(), getCurrentEditorDataContext());
        checkResultByFile(path + ".after");
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
