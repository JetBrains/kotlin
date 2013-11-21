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

package org.jetbrains.jet.completion;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.project.TargetPlatform;

public abstract class JetFixtureCompletionBaseTestCase extends LightCodeInsightFixtureTestCase {
    private boolean autocompleteSetting;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        autocompleteSetting = setAutocompleteSetting(false);
    }

    @Override
    protected void tearDown() throws Exception {
        setAutocompleteSetting(autocompleteSetting);

        super.tearDown();
    }

    private boolean setAutocompleteSetting(boolean value){
        CodeInsightSettings settings = CodeInsightSettings.getInstance();
        boolean oldValue;
        if (completionType() == CompletionType.SMART){
            oldValue = settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION;
            settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = value;
        }
        else{
            oldValue = settings.AUTOCOMPLETE_COMMON_PREFIX;
            settings.AUTOCOMPLETE_ON_CODE_COMPLETION = value;
        }
        return oldValue;
    }

    public abstract TargetPlatform getPlatform();

    @NotNull
    protected abstract CompletionType completionType();

    public void doTest(String testPath) {
        myFixture.configureByFile(testPath);

        String fileText = myFixture.getFile().getText();

        Integer invocationCount = ExpectedCompletionUtils.getInvocationCount(fileText);

        myFixture.complete(completionType(), invocationCount == null ? 0 : invocationCount);

        ExpectedCompletionUtils.assertDirectivesValid(fileText);

        ExpectedCompletionUtils.CompletionProposal[] expected = ExpectedCompletionUtils.itemsShouldExist(fileText, getPlatform());
        ExpectedCompletionUtils.CompletionProposal[] unexpected = ExpectedCompletionUtils.itemsShouldAbsent(fileText, getPlatform());
        Integer itemsNumber = ExpectedCompletionUtils.getExpectedNumber(fileText, getPlatform());

        assertTrue("Should be some assertions about completion",
                   expected.length != 0 || unexpected.length != 0 || itemsNumber != null);

        LookupElement[] items = myFixture.getLookupElements();

        if (items == null) {
            items = new LookupElement[0];
        }

        ExpectedCompletionUtils.assertContainsRenderedItems(expected, items, ExpectedCompletionUtils.isWithOrder(fileText));
        ExpectedCompletionUtils.assertNotContainsRenderedItems(unexpected, items);

        if (itemsNumber != null) {
            assertEquals(
                    String.format(
                            "Invalid number of completion items: %s",
                            ExpectedCompletionUtils.listToString(ExpectedCompletionUtils.getItemsInformation(items))),
                    itemsNumber.intValue(), items.length);
        }
    }
}
