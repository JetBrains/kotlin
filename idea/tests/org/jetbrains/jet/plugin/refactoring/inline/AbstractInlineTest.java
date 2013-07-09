package org.jetbrains.jet.plugin.refactoring.inline;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.intellij.codeInsight.TargetElementUtilBase.ELEMENT_NAME_ACCEPTED;
import static com.intellij.codeInsight.TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED;

public abstract class AbstractInlineTest extends LightCodeInsightFixtureTestCase {
    protected void doTest(@NotNull String path) throws IOException {
        File afterFile = new File(path + ".after");

        myFixture.configureByFile(path);

        boolean afterFileExists = afterFile.exists();

        final PsiElement targetElement =
                TargetElementUtilBase.findTargetElement(myFixture.getEditor(), ELEMENT_NAME_ACCEPTED | REFERENCED_ELEMENT_ACCEPTED);
        final KotlinInlineLocalHandler handler = new KotlinInlineLocalHandler();

        List<String> expectedErrors = InTextDirectivesUtils.findLinesWithPrefixesRemoved(myFixture.getFile().getText(), "// ERROR: ");
        if (handler.canInlineElement(targetElement)) {
            try {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        handler.inlineElement(myFixture.getProject(), myFixture.getEditor(), targetElement);
                    }
                });

                assertTrue(afterFileExists);
                assertEmpty(expectedErrors);
                myFixture.checkResult(FileUtil.loadFile(afterFile));
            } catch (CommonRefactoringUtil.RefactoringErrorHintException e) {
                assertFalse(afterFileExists);
                assertEquals(1, expectedErrors.size());
                assertEquals(expectedErrors.get(0).replace("\\n", "\n"), e.getMessage());
            }
        }
        else {
            assertFalse(afterFileExists);
        }
    }
}