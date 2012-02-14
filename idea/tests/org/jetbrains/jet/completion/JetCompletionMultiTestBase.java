/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.intellij.codeInsight.completion.CompletionTestCase;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.containers.HashSet;

import java.util.Set;

/**
 * @author Nikolay Krasko
 */
public abstract class JetCompletionMultiTestBase extends CompletionTestCase {

    abstract String[] getFileNameList();

    /**
     * @param completionLevel {@see CompletionParameters.getInvocationCount()} javadoc
     * @throws Exception
     */
    protected void doFileTest(int completionLevel) {
        try {
            configureByFiles(null, getFileNameList());
            complete(completionLevel);

            final String fileText = getFile().getText();
            final ExpectedCompletionUtils completionUtils = new ExpectedCompletionUtils();

            assertContainsItems(completionUtils.itemsShouldExist(fileText));
            assertNotContainItems(completionUtils.itemsShouldAbsent(fileText));

            Integer itemsNumber = completionUtils.getExpectedNumber(fileText);
            if (itemsNumber != null) {
                assertEquals(itemsNumber.intValue(), myItems.length);
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    protected void doFileTest() {
        doFileTest(1);
    }

    // Copied from com.intellij.codeInsight.completion.LightCompletionTestCase
    protected void assertContainsItems(final String... expected) {
        final Set<String> actual = getLookupStrings();
        for (String s : expected) {
            assertTrue("Expected '" + s + "' not found in " + actual,
                       actual.contains(s));
        }
    }

    // Copied from com.intellij.codeInsight.completion.LightCompletionTestCase
    protected void assertNotContainItems(final String... unexpected) {
        final Set<String> actual = getLookupStrings();
        for (String s : unexpected) {
            assertFalse("Unexpected '" + s + "' presented in " + actual,
                        actual.contains(s));
        }
    }

    // Copied from com.intellij.codeInsight.completion.LightCompletionTestCase
    private Set<String> getLookupStrings() {
        final Set<String> actual = new HashSet<String>();
        if (myItems != null) {
            for (LookupElement lookupElement : myItems) {
                actual.add(lookupElement.getLookupString());
            }
        }
        return actual;
    }
}
