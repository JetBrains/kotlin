/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.confidence;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.LightCompletionTestCase;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.completion.test.CompletionTestUtilKt;
import org.jetbrains.kotlin.idea.test.TestUtilsKt;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

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
                try {
                    Method m = null; //Looking for shouldSkipAutoPopup method (note, we can't use name because of scrambling in Ultimate)
                    for (Method method : CodeCompletionHandlerBase.class.getDeclaredMethods()) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (method.getReturnType().equals(Boolean.TYPE) &&
                            parameterTypes.length == 2 &&
                            parameterTypes[0].equals(Editor.class) &&
                            parameterTypes[1].equals(PsiFile.class)) {
                            assertNull("Only one method with such signature should exist", m);
                            m = method;
                        }
                    }
                    assertNotNull(m);
                    m.setAccessible(true);
                    Object o = m.invoke(null, getEditor(), getFile());
                    assert o instanceof Boolean;
                    assertTrue("Should skip autopopup completion", ((Boolean) o).booleanValue());
                } catch (Throwable e) {
                    throw new AssertionError(e);
                }
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
                getProject(), getEditor(), 0, false, false);

        LookupImpl lookup = (LookupImpl) LookupManager.getActiveLookup(myEditor);
        myItems = lookup == null ? null : lookup.getItems().toArray(LookupElement.EMPTY_ARRAY);
        myPrefix = lookup == null ? null : lookup.itemPattern(lookup.getItems().get(0));
    }
}
