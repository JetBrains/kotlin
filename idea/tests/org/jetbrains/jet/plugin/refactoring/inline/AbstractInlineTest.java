package org.jetbrains.jet.plugin.refactoring.inline;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

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

        assertEquals(afterFileExists, handler.canInlineElement(targetElement));
        if (!afterFileExists) {
            return;
        }

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                handler.inlineElement(myFixture.getProject(), myFixture.getEditor(), targetElement);
            }
        });
        myFixture.checkResult(FileUtil.loadFile(afterFile));
    }
}