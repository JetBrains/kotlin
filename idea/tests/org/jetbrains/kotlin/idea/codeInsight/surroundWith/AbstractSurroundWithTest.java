/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.surroundWith;

import com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler;
import com.intellij.lang.LanguageSurrounders;
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.expression.*;
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.statement.*;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;
import java.util.List;

public abstract class AbstractSurroundWithTest extends LightCodeInsightTestCase {

    public void doTestWithIfSurrounder(String path) throws Exception {
        doTest(path, new KotlinIfSurrounder());
    }

    public void doTestWithIfElseSurrounder(String path) throws Exception {
        doTest(path, new KotlinIfElseSurrounder());
    }

    public void doTestWithIfElseExpressionSurrounder(String path) throws Exception {
        doTest(path, new KotlinIfElseExpressionSurrounder(false));
    }

    public void doTestWithIfElseExpressionBracesSurrounder(String path) throws Exception {
        doTest(path, new KotlinIfElseExpressionSurrounder(true));
    }

    public void doTestWithNotSurrounder(String path) throws Exception {
        doTest(path, new KotlinNotSurrounder());
    }

    public void doTestWithParenthesesSurrounder(String path) throws Exception {
        doTest(path, new KotlinParenthesesSurrounder());
    }

    public void doTestWithStringTemplateSurrounder(String path) throws Exception {
        doTest(path, new KotlinStringTemplateSurrounder());
    }

    public void doTestWithWhenSurrounder(String path) throws Exception {
        doTest(path, new KotlinWhenSurrounder());
    }

    public void doTestWithTryCatchSurrounder(String path) throws Exception {
        doTest(path, new KotlinTryCatchSurrounder());
    }

    public void doTestWithTryCatchExpressionSurrounder(String path) throws Exception {
        doTest(path, new KotlinTryExpressionSurrounder.TryCatch());
    }

    public void doTestWithTryCatchFinallySurrounder(String path) throws Exception {
        doTest(path, new KotlinTryCatchFinallySurrounder());
    }

    public void doTestWithTryCatchFinallyExpressionSurrounder(String path) throws Exception {
        doTest(path, new KotlinTryExpressionSurrounder.TryCatchFinally());
    }

    public void doTestWithTryFinallySurrounder(String path) throws Exception {
        doTest(path, new KotlinTryFinallySurrounder());
    }

    public void doTestWithFunctionLiteralSurrounder(String path) throws Exception {
        doTest(path, new KotlinFunctionLiteralSurrounder());
    }

    public void doTestWithSurroundWithIfExpression(String path) throws Exception {
        doTest(path, new KotlinWithIfExpressionSurrounder(false));
    }

    public void doTestWithSurroundWithIfElseExpression(String path) throws Exception {
        doTest(path, new KotlinWithIfExpressionSurrounder(true));
    }

    private void doTest(String path, Surrounder surrounder) throws Exception {
        configureByFile(path);

        String fileText = FileUtil.loadFile(new File(path), true);
        String isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// IS_APPLICABLE: ");

        if (isApplicableString != null) {
            boolean isApplicableExpected = toString().equals("true");
            PsiElement[] elementsToSurround = getElementsToSurround(surrounder);
            assert elementsToSurround != null : "Couldn't find elements to surround";
            assert isApplicableExpected == surrounder.isApplicable(elementsToSurround)
                    : "isApplicable() for " + surrounder.getClass() + " should return " + isApplicableExpected;
            if (isApplicableExpected) {
                invokeSurroundAndCheck(path, surrounder);
            }
        }
        else {
            invokeSurroundAndCheck(path, surrounder);
        }
    }

    private void invokeSurroundAndCheck(@NotNull String path, @NotNull Surrounder surrounder) {
        SurroundWithHandler.invoke(getProject(), getEditor(), getFile(), surrounder);

        String filePath = path + ".after";
        try {
            checkResultByFile(filePath);
        }
        catch (FileComparisonFailure fileComparisonFailure) {
            KotlinTestUtils.assertEqualsToFile(new File(filePath), getEditor());
        }
    }

    @Nullable
    private PsiElement[] getElementsToSurround(@NotNull Surrounder surrounder) {
        List<SurroundDescriptor> surroundDescriptors =
                LanguageSurrounders.INSTANCE.allForLanguage(getFile().getViewProvider().getBaseLanguage());

        String surrounderDescription = surrounder.getTemplateDescription();
        for (SurroundDescriptor descriptor : surroundDescriptors) {
            Surrounder[] surrounders = descriptor.getSurrounders();
            for (Surrounder surrounderInDescriptor : surrounders) {
                if (surrounderInDescriptor.getTemplateDescription().equals(surrounderDescription)) {
                    SelectionModel selection = getEditor().getSelectionModel();
                    PsiElement[] elements = descriptor.getElementsToSurround(
                            getFile(), selection.getSelectionStart(), selection.getSelectionEnd());
                    return elements;
                }
            }
        }

        return null;
    }

    @Override
    protected boolean isRunInWriteAction() {
        return true;
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
