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

/**
 * @author Nikolay Krasko
 */
public abstract class JetCompletionMultiTestBase extends CompletionTestCase {

    abstract String[] getFileNameList();

    /**
     * @param completionLevel {@see CompletionParameters.getInvocationCount()} javadoc
     * @param fileNameList
     * @throws Exception
     */
    protected void doFileTest(int completionLevel, String[] fileNameList) {
        try {
            configureByFiles(null, fileNameList);
            complete(completionLevel);

            final String fileText = getFile().getText();
            final ExpectedCompletionUtils completionUtils = new ExpectedCompletionUtils();

            ExpectedCompletionUtils.assertContainsRenderedItems(
                    completionUtils.itemsShouldExist(fileText), myItems, completionUtils.isWithOrder(fileText));

            ExpectedCompletionUtils.assertNotContainsRenderedItems(completionUtils.itemsShouldAbsent(fileText), myItems);

            Integer itemsNumber = completionUtils.getExpectedNumber(fileText);
            if (itemsNumber != null) {
                assertEquals(itemsNumber.intValue(), myItems.length);
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    protected void doFileTest(int completionLevel) {
        doFileTest(completionLevel, getFileNameList());
    }

    protected void doFileTest() {
        doFileTest(1, getFileNameList());
    }
}
