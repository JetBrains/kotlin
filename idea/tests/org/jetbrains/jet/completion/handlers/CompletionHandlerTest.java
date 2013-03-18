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

package org.jetbrains.jet.completion.handlers;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupEvent;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.formatter.JetCodeStyleSettings;

import java.io.File;

public class CompletionHandlerTest extends LightCodeInsightFixtureTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ((StartupManagerImpl) StartupManager.getInstance(getProject())).runPostStartupActivities();
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    public void testClassCompletionImport() {
        doTest(CompletionType.BASIC, 2, "SortedSet", null, '\n');
    }

    public void testDoNotInsertImportForAlreadyImported() {
        doTest();
    }

    public void testDoNotInsertImportIfResolvedIntoJavaConstructor() {
        doTest();
    }

    public void testNonStandardArray() {
        doTest(CompletionType.BASIC, 2, "Array", "java.lang.reflect", '\n');
    }

    public void testNoParamsFunction() {
        doTest();
    }

    public void testParamsFunction() {
        doTest();
    }

    public void testInsertJavaClassImport() {
        doTest();
    }

    public void testInsertVoidJavaMethod() {
        doTest();
    }

    public void testPropertiesSetter() {
        doTest();
    }

    public void testSingleBrackets() {
        myFixture.configureByFile(getBeforeFileName());
        myFixture.type('(');
        myFixture.checkResultByFile(getAfterFileName());
    }

    public void testExistingSingleBrackets() {
        doTest();
    }

    public void testExtFunction() {
        doTest();
    }

    public void testFunctionLiteralInsertOnSpace() {
        doTest(CompletionType.BASIC, 2, null, null, ' ');
    }

    public void testInsertFunctionWithBothParentheses() {
        myFixture.configureByFile(getBeforeFileName());
        myFixture.type("test()");
        myFixture.checkResultByFile(getAfterFileName());
    }

    public void testInsertImportOnTab() {
        doTest(CompletionType.BASIC, 2, "ArrayList", null, '\t');
    }

    public void testFunctionLiteralInsertWhenNoSpacesForBraces() {
        CodeStyleSettings settings = CodeStyleSettingsManager.getSettings(getProject());
        JetCodeStyleSettings jetSettings = settings.getCustomSettings(JetCodeStyleSettings.class);

        try {
            jetSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD = false;
            doTest();
        } finally {
            jetSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD = true;
        }
    }

    public void testHigherOrderFunction() {
        doTest();
    }

    public void testInsertFqnForJavaClass() {
        doTest(CompletionType.BASIC, 2, "SortedSet", "java.util", '\n');
    }

    public void testHigherOrderFunctionWithArg() {
        doTest(CompletionType.BASIC, 2, "filterNot", null, '\n');
    }

    public void doTest() {
        doTest(CompletionType.BASIC, 2, null, null, '\n');
    }

    public void doTest(CompletionType type, int time, @Nullable String lookupString, @Nullable String tailText, char completionChar) {
        myFixture.configureByFile(getBeforeFileName());

        if (lookupString != null || tailText != null) {
            myFixture.complete(type, time);

            LookupElement item = getExistentLookupElement(lookupString, tailText);
            if (item != null) {
                selectItem(item, completionChar);
            }
        }
        else {
            forceCompleteFirst(type, time);
        }

        myFixture.checkResultByFile(getAfterFileName());
    }

    @Nullable
    public LookupElement getExistentLookupElement(@Nullable String lookupString, @Nullable String tailText) {
        LookupImpl lookup = (LookupImpl) LookupManager.getInstance(getProject()).getActiveLookup();
        LookupElement foundElement = null;

        if (lookup != null) {
            LookupElementPresentation presentation = new LookupElementPresentation();

            for (LookupElement lookupElement : lookup.getItems()) {
                boolean lookupOk;

                if (lookupString != null) {
                    lookupOk = (lookupElement.getLookupString().contains(lookupString));
                }
                else {
                    lookupOk = true;
                }

                boolean tailOk;

                if (tailText != null) {
                    lookupElement.renderElement(presentation);
                    String itemTailText = presentation.getTailText();
                    tailOk = itemTailText != null && itemTailText.contains(tailText);
                }
                else {
                    tailOk = true;
                }


                if (lookupOk && tailOk) {
                    if (foundElement != null) {
                        Assert.fail("Several elements satisfy to completion restrictions");
                    }
                    foundElement = lookupElement;
                }
            }
        }

        return foundElement;
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
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/handlers/").getPath() + File.separator;
    }

    protected void forceCompleteFirst(CompletionType type, int time) {
        myFixture.complete(type, time);
        LookupElement[] items = myFixture.getLookupElements();
        if (items != null && items.length > 1) {
            selectItem(items[0]);
        }
    }

    protected void selectItem(LookupElement item) {
        selectItem(item, (char)0);
    }

    protected void selectItem(LookupElement item, final char completionChar) {
        final LookupImpl lookup = (LookupImpl) myFixture.getLookup();
        lookup.setCurrentItem(item);
        if (LookupEvent.isSpecialCompletionChar(completionChar)) {
            new WriteCommandAction.Simple(getProject()) {
                @Override
                protected void run() throws Throwable {
                    lookup.finishLookup(completionChar);
                }
            }.execute().throwException();
        } else {
            myFixture.type(completionChar);
        }
    }
}
