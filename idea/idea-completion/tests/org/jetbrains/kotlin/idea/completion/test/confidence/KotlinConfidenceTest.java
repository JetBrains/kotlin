/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.completion.test.confidence;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.LightCompletionTestCase;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.completion.test.CompletionTestUtilKt;
import org.jetbrains.kotlin.idea.test.RunnableWithException;
import org.jetbrains.kotlin.idea.test.TestUtilsKt;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

import java.io.File;
import java.util.List;

public class KotlinConfidenceTest extends LightCompletionTestCase {
    private static final String TYPE_DIRECTIVE_PREFIX = "// TYPE:";

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

    @Override
    protected void tearDown() throws Exception {
        TestUtilsKt.doKotlinTearDown(getProject(), new RunnableWithException() {
            @Override
            public void run() throws Exception {
                KotlinConfidenceTest.super.tearDown();
            }
        });
    }

    protected void doTest() {
        boolean completeByChars = CodeInsightSettings.getInstance().SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS;

        CodeInsightSettings.getInstance().SELECT_AUTOPOPUP_SUGGESTIONS_BY_CHARS = true;

        try {
            configureByFile(getBeforeFileName());
            String text = getEditor().getDocument().getText();
            String typeText = getTypeText(text);
            List<String> expectedElements = InTextDirectivesUtils.findLinesWithPrefixesRemoved(text, "// ELEMENT:");
            boolean noLookup = InTextDirectivesUtils.isDirectiveDefined(text, "// NO_LOOKUP");

            assertFalse("Can't both expect lookup elements and no lookup", !expectedElements.isEmpty() && noLookup);

            if (noLookup) {
                assertNull("Expected no lookup", getLookup());
                return;
            }
            else if (!expectedElements.isEmpty()) {
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

    protected static String getTypeText(String text) {

        String[] directives = InTextDirectivesUtils.findArrayWithPrefixes(text, TYPE_DIRECTIVE_PREFIX);
        if (directives.length == 0) return null;
        assertEquals("One directive with \"" + TYPE_DIRECTIVE_PREFIX +"\" expected", 1, directives.length);

        return StringUtil.unquoteString(directives[0]);
    }

    protected String getBeforeFileName() {
        return getTestName(false) + ".kt";
    }

    protected String getAfterFileName() {
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
        new CodeCompletionHandlerBase(CompletionType.BASIC, false, true, true).invokeCompletion(
                getProject(), getEditor(), 0, false, false);

        LookupImpl lookup = (LookupImpl) LookupManager.getActiveLookup(myEditor);
        myItems = lookup == null ? null : lookup.getItems().toArray(LookupElement.EMPTY_ARRAY);
        myPrefix = lookup == null ? null : lookup.itemPattern(lookup.getItems().get(0));
    }
}
