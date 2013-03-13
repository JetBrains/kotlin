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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.InTextDirectivesUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract a number of statements about completion from the given text. Those statements
 * should be asserted during test execution.
 */
public class ExpectedCompletionUtils {

    public static class CompletionProposal {
        public static final Pattern PATTERN = Pattern.compile("([^~@]*)(@([^~]*))?(~(.*))?");
        public static final int LOOKUP_STRING_GROUP_INDEX = 1;
        public static final int PRESENTABLE_STRING_GROUP_INDEX = 3;
        public static final int TAIL_TEXT_STRING_GROUP_INDEX = 5;

        public static final String TAIL_FLAG = "~";
        public static final String PRESENTABLE_FLAG = "@";

        private final String lookupString;
        private final String presenterText;
        private final String tailString;

        public CompletionProposal(@NotNull String lookupString, @Nullable String presenterText, @Nullable String tailString) {
            this.lookupString = lookupString;
            this.presenterText = presenterText != null ? presenterText.trim() : null;
            this.tailString = tailString != null ? tailString.trim() : null;
        }

        public boolean isSuitable(CompletionProposal proposal) {
            if (proposal.tailString != null) {
                if (!proposal.tailString.equals(tailString)) {
                    return false;
                }
            }

            if (proposal.presenterText != null) {
                if (!proposal.presenterText.equals(presenterText)) {
                    return false;
                }
            }

            return lookupString.equals(proposal.lookupString);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(lookupString);
            if (presenterText != null) {
                result.append(PRESENTABLE_FLAG).append(presenterText);
            }

            if (tailString != null) {
                result.append(TAIL_FLAG).append(tailString);
            }

            return result.toString();
        }
    }
    
    public static final String EXIST_LINE_PREFIX = "// EXIST:";
    public static final String ABSENT_LINE_PREFIX = "// ABSENT:";
    public static final String NUMBER_LINE_PREFIX = "// NUMBER:";
    public static final String EXECUTION_TIME_PREFIX = "// TIME:";
    public static final String WITH_ORDER_PREFIX = "// WITH_ORDER:";

    private final String existLinePrefix;
    private final String absentLinePrefix;
    private final String numberLinePrefix;
    private final String executionTimePrefix;
    private final String withOrderPrefix;

    public ExpectedCompletionUtils() {
        this(EXIST_LINE_PREFIX, ABSENT_LINE_PREFIX, NUMBER_LINE_PREFIX, EXECUTION_TIME_PREFIX, WITH_ORDER_PREFIX);
    }
    
    public ExpectedCompletionUtils(String existLinePrefix, String absentLinePrefix,
            String numberLinePrefix, String executionTimePrefix, String withOrderPrefix) {
        this.existLinePrefix = existLinePrefix;
        this.absentLinePrefix = absentLinePrefix;
        this.numberLinePrefix = numberLinePrefix;
        this.executionTimePrefix = executionTimePrefix;
        this.withOrderPrefix = withOrderPrefix;
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
        Collection<CompletionProposal> proposals = new ArrayList<CompletionProposal>();
        for (String proposalStr : InTextDirectivesUtils.findListWithPrefix(prefix, fileText)) {
            Matcher matcher = CompletionProposal.PATTERN.matcher(proposalStr);
            matcher.find();
            proposals.add(new CompletionProposal(matcher.group(CompletionProposal.LOOKUP_STRING_GROUP_INDEX),
                                                 matcher.group(CompletionProposal.PRESENTABLE_STRING_GROUP_INDEX),
                                                 matcher.group(CompletionProposal.TAIL_TEXT_STRING_GROUP_INDEX)));
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

    public boolean isWithOrder(String fileText) {
        return InTextDirectivesUtils.getPrefixedInt(fileText, withOrderPrefix) != null;
    }

    protected static void assertContainsRenderedItems(CompletionProposal[] expected, LookupElement[] items, boolean checkOrder) {
        List<CompletionProposal> itemsInformation = getItemsInformation(items);
        String allItemsString = listToString(itemsInformation);

        int indexOfPrevious = Integer.MIN_VALUE;

        for (CompletionProposal expectedProposal : expected) {
            boolean isFound = false;

            for (int index = 0; index < itemsInformation.size(); index++) {
                CompletionProposal proposal = itemsInformation.get(index);

                if (proposal.isSuitable(expectedProposal)) {
                    isFound = true;

                    Assert.assertTrue("Invalid order of existent elements in " + allItemsString,
                                      !checkOrder || index > indexOfPrevious);
                    indexOfPrevious = index;

                    break;
                }
            }

            Assert.assertTrue("Expected '" + expectedProposal + "' not found in " + allItemsString, isFound);
        }
    }

    protected static void assertNotContainsRenderedItems(CompletionProposal[] unexpected,LookupElement[] items) {
        List<CompletionProposal> itemsInformation = getItemsInformation(items);
        String allItemsString = listToString(itemsInformation);

        for (CompletionProposal unexpectedProposal : unexpected) {
            for (CompletionProposal proposal : itemsInformation) {
                Assert.assertFalse("Unexpected '" + unexpectedProposal + "' presented in " + allItemsString,
                                   proposal.isSuitable(unexpectedProposal));
            }
        }
    }

    protected static List<CompletionProposal> getItemsInformation(LookupElement[] items) {
        LookupElementPresentation presentation = new LookupElementPresentation();

        List<CompletionProposal> result = new ArrayList<CompletionProposal>();
        if (items != null) {
            for (LookupElement item : items) {
                item.renderElement(presentation);
                result.add(new ExpectedCompletionUtils.CompletionProposal(item.getLookupString(), presentation.getItemText(),
                                                                          presentation.getTailText()));
            }
        }

        return result;
    }

    protected static String listToString(Collection<CompletionProposal> items) {
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
