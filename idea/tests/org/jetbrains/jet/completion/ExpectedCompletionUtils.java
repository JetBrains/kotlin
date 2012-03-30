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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.testing.InTextDirectivesUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Extract a number of statements about completion from the given text. Those statements
 * should be asserted during test execution.
 *
 * @author Nikolay Krasko
 */
public class ExpectedCompletionUtils {

    public static class CompletionProposal {
        public static final String TAIL_FLAG = "~";

        private final String lookupString;
        private final String tailString;

        public CompletionProposal(@NotNull String lookupString, @Nullable String tailString) {
            this.lookupString = lookupString;
            this.tailString = tailString != null ? tailString.trim() : null;
        }

        public boolean isSuitable(CompletionProposal proposal) {
            if (proposal.tailString != null) {
                if (!proposal.tailString.equals(tailString)) {
                    return false;
                }
            }

            return lookupString.equals(proposal.lookupString);
        }

        @Override
        public String toString() {
            if (tailString != null) {
                return lookupString + TAIL_FLAG + tailString;
            }

            return lookupString;
        }
    }
    
    public static final String EXIST_LINE_PREFIX = "// EXIST:";
    public static final String ABSENT_LINE_PREFIX = "// ABSENT:";
    public static final String NUMBER_LINE_PREFIX = "// NUMBER:";
    public static final String EXECUTION_TIME_PREFIX = "// TIME:";

    private final String existLinePrefix;
    private final String absentLinePrefix;
    private final String numberLinePrefix;
    private final String executionTimePrefix;

    public ExpectedCompletionUtils() {
        this(EXIST_LINE_PREFIX, ABSENT_LINE_PREFIX, NUMBER_LINE_PREFIX, EXECUTION_TIME_PREFIX);
    }
    
    public ExpectedCompletionUtils(String existLinePrefix, String absentLinePrefix, String numberLinePrefix, String execitionTimePrefix) {
        this.existLinePrefix = existLinePrefix;
        this.absentLinePrefix = absentLinePrefix;
        this.numberLinePrefix = numberLinePrefix;
        this.executionTimePrefix = execitionTimePrefix;
    }

    @NotNull
    public CompletionProposal[] itemsShouldExist(String fileText) {
        return processProposalAssertions(existLinePrefix, fileText);
    }

    @NotNull
    public CompletionProposal[] itemsShouldAbsent(String fileText) {
        return processProposalAssertions(absentLinePrefix, fileText);
    }

    public static CompletionProposal[] processProposalAssertions(String prefix, String fileText) {
        List<CompletionProposal> proposals = new ArrayList<CompletionProposal>();
        for (String proposalStr : InTextDirectivesUtils.findListWithPrefix(prefix, fileText)) {
            int tailChar = proposalStr.indexOf(CompletionProposal.TAIL_FLAG);

            if (tailChar > 0) {
                proposals.add(new CompletionProposal(proposalStr.substring(0, tailChar),
                                                     proposalStr.substring(tailChar + 1, proposalStr.length())));
            }
            else {
                proposals.add(new CompletionProposal(proposalStr, null));
            }
        }

        return ArrayUtil.toObjectArray(proposals, CompletionProposal.class);
    }

    @Nullable
    public Integer getExpectedNumber(String fileText) {
        return InTextDirectivesUtils.getPrefixedInt(fileText, numberLinePrefix);
    }

    @Nullable
    public Integer getExecutionTime(String fileText) {
        return InTextDirectivesUtils.getPrefixedInt(fileText, executionTimePrefix);
    }

    protected static void assertContainsRenderedItems(CompletionProposal[] expected, LookupElement[] items) {
        List<CompletionProposal> itemsInformation = getItemsInformation(items);

        for (CompletionProposal expectedProposal : expected) {
            boolean isFound = false;

            for (CompletionProposal proposal : itemsInformation) {
                if (proposal.isSuitable(expectedProposal)) {
                    isFound = true;
                    break;
                }
            }

            Assert.assertTrue("Expected '" + expectedProposal + "' not found in " + listToString(itemsInformation), isFound);
        }
    }

    protected static void assertNotContainsRenderedItems(CompletionProposal[] unexpected,LookupElement[] items) {
        List<CompletionProposal> itemsInformation = getItemsInformation(items);

        for (CompletionProposal unexpectedProposal : unexpected) {
            for (CompletionProposal proposal : itemsInformation) {
                Assert.assertFalse("Unexpected '" + unexpectedProposal + "' presented in " + listToString(itemsInformation),
                                   proposal.isSuitable(unexpectedProposal));
            }
        }
    }

    protected static List<CompletionProposal> getItemsInformation(LookupElement[] items) {
        final LookupElementPresentation presentation = new LookupElementPresentation();

        List<CompletionProposal> result = new ArrayList<CompletionProposal>();
        if (items != null) {
            for (LookupElement item : items) {
                item.renderElement(presentation);
                result.add(new ExpectedCompletionUtils.CompletionProposal(item.getLookupString(), presentation.getTailText()));
            }
        }

        return result;
    }

    protected static String listToString(List<ExpectedCompletionUtils.CompletionProposal> items) {
        return StringUtil.join(
            Collections2.transform(items, new Function<CompletionProposal, String>() {
                @Override
                public String apply(@Nullable CompletionProposal proposal) {
                    assert proposal != null;
                    return proposal.toString();
                }
            }), "\n");
    }
}
