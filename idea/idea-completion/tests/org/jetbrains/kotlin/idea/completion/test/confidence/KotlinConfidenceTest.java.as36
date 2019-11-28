/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.confidence;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.lang.Language;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ThreeState;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.completion.test.CompletionTestUtilKt;
import org.jetbrains.kotlin.idea.test.TestUtilsKt;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

@RunWith(JUnit3WithIdeaConfigurationRunner.class)
public class KotlinConfidenceTest extends LightCompletionTestCase {
    private static final String TYPE_DIRECTIVE_PREFIX = "// TYPE:";
    private final ThreadLocal<Boolean> skipComplete = ThreadLocal.withInitial(() -> false);

    public void testCompleteOnDotOutOfRanges() {
        doTest();
    }

    public void testImportAsConfidence() {
        doTest();
    }

    public void testInBlockOfFunctionLiteral() {
        doTest();
    }

    public void testInModifierList() {
        doTest();
    }

    public void testNoAutoCompletionForRangeOperator() {
        doTest();
    }

    public void testNoAutoPopupInString() {
        doTest();
    }

    public void testAutoPopupInStringTemplate() {
        doTest();
    }

    public void testAutoPopupInStringTemplateAfterDollar() {
        doTest();
    }

    public void testNoAutoPopupInStringTemplateAfterSpace() {
        doTest();
    }

    public void testNoAutoPopupInRawStringTemplateAfterNewLine() {
        doTest();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestUtilsKt.invalidateLibraryCache(getProject());
    }

    protected void doTest() {
        boolean completeByChars = CodeInsightSettings.getInstance().SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS;

        CodeInsightSettings.getInstance().SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS = true;

        try {
            skipComplete.set(true);
            try {
                configureByFile(getBeforeFileName());
            } finally {
                skipComplete.set(false);
            }

            String text = getEditor().getDocument().getText();
            boolean noLookup = InTextDirectivesUtils.isDirectiveDefined(text, "// NO_LOOKUP");
            if (!noLookup) complete(); //This will cause NPE in case of absence of autopopup completion

            List<String> expectedElements = InTextDirectivesUtils.findLinesWithPrefixesRemoved(text, "// ELEMENT:");
            assertFalse("Can't both expect lookup elements and no lookup", !expectedElements.isEmpty() && noLookup);

            if (noLookup) {
                assertTrue("Should skip autopopup completion", shouldSkipAutoPopup(getEditor(), getFile()));
                return;
            }

            String typeText = getTypeText(text);
            if (!expectedElements.isEmpty()) {
                assertContainsItems(ArrayUtil.toStringArray(expectedElements));
                return;
            }
            assertNotNull("You must type something, use // TYPE:", typeText);
            type(typeText);
            checkResultByFile(getAfterFileName());
        }
        finally {
            CodeInsightSettings.getInstance().SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS = completeByChars;
        }
    }

    private static String getTypeText(String text) {
        String[] directives = InTextDirectivesUtils.findArrayWithPrefixes(text, TYPE_DIRECTIVE_PREFIX);
        if (directives.length == 0) return null;
        assertEquals("One directive with \"" + TYPE_DIRECTIVE_PREFIX +"\" expected", 1, directives.length);

        return StringUtil.unquoteString(directives[0]);
    }

    private String getBeforeFileName() {
        return getTestName(false) + ".kt";
    }

    private String getAfterFileName() {
        return getTestName(false) + ".kt.after";
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return new File(CompletionTestUtilKt.getCOMPLETION_TEST_DATA_BASE_PATH(), "/confidence/").getPath() + File.separator;
    }

    @Override
    protected Sdk getProjectJDK() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }

    @Override
    protected void complete() {
        if (skipComplete.get()) return;
        new CodeCompletionHandlerBase(CompletionType.BASIC, false, true, true).invokeCompletion(
                getProject(), getEditor(), 0, false);

        LookupImpl lookup = (LookupImpl) LookupManager.getActiveLookup(myEditor);
        myItems = lookup == null ? null : lookup.getItems().toArray(LookupElement.EMPTY_ARRAY);
        myPrefix = lookup == null ? null : lookup.itemPattern(lookup.getItems().get(0));
    }

    private static boolean shouldSkipAutoPopup(Editor editor, PsiFile psiFile) {
        int offset = editor.getCaretModel().getOffset();
        int psiOffset = Math.max(0, offset - 1);

        PsiElement elementAt = InjectedLanguageManager.getInstance(psiFile.getProject()).findInjectedElementAt(psiFile, psiOffset);
        if (elementAt == null) {
            elementAt = psiFile.findElementAt(psiOffset);
        }
        if (elementAt == null) return true;

        Language language = PsiUtilCore.findLanguageFromElement(elementAt);

        for (CompletionConfidence confidence : CompletionConfidenceEP.forLanguage(language)) {
            ThreeState result = confidence.shouldSkipAutopopup(elementAt, psiFile, offset);
            if (result != ThreeState.UNSURE) {
                return result == ThreeState.YES;
            }
        }

        return false;
    }
}
