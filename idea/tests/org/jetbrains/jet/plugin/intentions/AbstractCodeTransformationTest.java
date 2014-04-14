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
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.LightCodeInsightTestCase;
import com.intellij.util.PathUtil;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.DirectiveBasedActionUtils;
import org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions.*;
import org.jetbrains.jet.plugin.intentions.declarations.ConvertMemberToExtension;
import org.jetbrains.jet.plugin.intentions.declarations.SplitPropertyDeclarationIntention;
import org.jetbrains.jet.testing.ConfigLibraryUtil;
import org.junit.Assert;

import java.io.File;

public abstract class AbstractCodeTransformationTest extends LightCodeInsightTestCase {
    public void doTestDoubleBangToIfThen(@NotNull String path) throws Exception {
        doTestIntention(path, new DoubleBangToIfThenIntention());
    }

    public void doTestIfThenToDoubleBang(@NotNull String path) throws Exception {
        doTestIntention(path, new IfThenToDoubleBangIntention());
    }

    public void doTestElvisToIfThen(@NotNull String path) throws Exception {
        doTestIntention(path, new ElvisToIfThenIntention());
    }

    public void doTestIfThenToElvis(@NotNull String path) throws Exception {
        doTestIntention(path, new IfThenToElvisIntention());
    }

    public void doTestSafeAccessToIfThen(@NotNull String path) throws Exception {
        doTestIntention(path, new SafeAccessToIfThenIntention());
    }

    public void doTestIfThenToSafeAccess(@NotNull String path) throws Exception {
        doTestIntention(path, new IfThenToSafeAccessIntention());
    }

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

    public void doTestSwapBinaryExpression(@NotNull String path) throws Exception {
        doTestIntention(path, new SwapBinaryExpression());
    }

    public void doTestRemoveExplicitTypeArguments(@NotNull String path) throws Exception {
        doTestIntention(path, new RemoveExplicitTypeArguments());
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

    public void doTestConvertNegatedExpressionWithDemorgansLaw(@NotNull String path) throws Exception {
        doTestIntention(path, new ConvertNegatedExpressionWithDemorgansLawIntention());
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

    public void doTestConvertNegatedBooleanSequence(@NotNull String path) throws Exception {
		 doTestIntention(path, new ConvertNegatedBooleanSequenceIntention());
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

    public void doTestSimplifyNegatedBinaryExpressionIntention(@NotNull String path) throws Exception {
        doTestIntention(path, new SimplifyNegatedBinaryExpressionIntention());
    }

    public void doTestInsertExplicitTypeArguments(@NotNull String path) throws Exception {
        doTestIntention(path, new InsertExplicitTypeArguments());
    }

    public void doTestSplitIf(@NotNull String path) throws Exception {
        doTestIntention(path, new SplitIfIntention());
    }

    public void doTestReplaceWithOperatorAssign(@NotNull String path) throws Exception {
        doTestIntention(path, new ReplaceWithOperatorAssignIntention());
    }

    public void doTestReplaceWithTraditionalAssignment(@NotNull String path) throws Exception {
        doTestIntention(path, new ReplaceWithTraditionalAssignmentIntention());
    }

    public void doTestSimplifyBooleanWithConstants(@NotNull String path) throws Exception {
        doTestIntention(path, new SimplifyBooleanWithConstantsIntention());
    }

    private void doTestIntention(@NotNull String path, @NotNull IntentionAction intentionAction) throws Exception {
        configureByFile(path);

        String fileText = FileUtil.loadFile(new File(path), true);
        boolean isWithRuntime = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// WITH_RUNTIME") != null;

        try {
            if (isWithRuntime) {
                ConfigLibraryUtil.configureKotlinRuntime(getModule(), getFullJavaJDK());
            }

            DirectiveBasedActionUtils.checkForUnexpectedErrors((JetFile) getFile());

            doTestFor(path, intentionAction, fileText);
        }
        finally {
            if (isWithRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntime(getModule(), getProjectJDK());
            }
        }
    }

    private void doTestFor(String path, IntentionAction intentionAction, String fileText) {
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
                    String canonicalPathToExpectedFile = PathUtil.getCanonicalPath(path + ".after");
                    checkResultByFile(canonicalPathToExpectedFile);
                }
            }
            assertNull("Expected test to fail.", shouldFailString);
        }
        catch (IntentionTestException e) {
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

    protected static Sdk getFullJavaJDK() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }
}
