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
import com.google.common.collect.ImmutableList;
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
    private ExpectedCompletionUtils() {
    }

    enum Platform {
        JAVA,
        JS,
        ALL
    }

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
    
    private static final String EXIST_LINE_PREFIX = "EXIST:";

    private static final String ABSENT_LINE_PREFIX = "ABSENT:";
    private static final String ABSENT_JS_LINE_PREFIX = "ABSENT_JS:";
    private static final String ABSENT_JAVA_LINE_PREFIX = "ABSENT_JAVA:";

    private static final String EXIST_JAVA_ONLY_LINE_PREFIX = "EXIST_JAVA_ONLY:";
    private static final String EXIST_JS_ONLY_LINE_PREFIX = "EXIST_JS_ONLY:";

    private static final String NUMBER_LINE_PREFIX = "NUMBER:";
    private static final String NUMBER_JS_LINE_PREFIX = "NUMBER_JS:";
    private static final String NUMBER_JAVA_LINE_PREFIX = "NUMBER_JAVA:";

    private static final String EXECUTION_TIME_PREFIX = "TIME:";
    private static final String WITH_ORDER_PREFIX = "WITH_ORDER:";

    public static final List<String> KNOWN_PREFIXES = ImmutableList.of(
            EXIST_LINE_PREFIX,
            ABSENT_LINE_PREFIX,
            ABSENT_JS_LINE_PREFIX,
            ABSENT_JAVA_LINE_PREFIX,
            EXIST_JAVA_ONLY_LINE_PREFIX,
            EXIST_JS_ONLY_LINE_PREFIX,
            NUMBER_LINE_PREFIX,
            NUMBER_JS_LINE_PREFIX,
            NUMBER_JAVA_LINE_PREFIX,
            EXECUTION_TIME_PREFIX,
            WITH_ORDER_PREFIX);

    @NotNull
    public static CompletionProposal[] itemsShouldExist(String fileText, Platform platform) {
        switch (platform) {
            case ALL:
                return processProposalAssertions(fileText, EXIST_LINE_PREFIX);
            case JAVA:
                return processProposalAssertions(fileText, EXIST_LINE_PREFIX, EXIST_JAVA_ONLY_LINE_PREFIX);
            case JS:
                return processProposalAssertions(fileText, EXIST_LINE_PREFIX, EXIST_JS_ONLY_LINE_PREFIX);
        }

        throw new IllegalArgumentException("platform");
    }

    @NotNull
    public static CompletionProposal[] itemsShouldAbsent(String fileText, Platform platform) {
        switch (platform) {
            case ALL:
                return processProposalAssertions(fileText, ABSENT_LINE_PREFIX);
            case JAVA:
                return processProposalAssertions(fileText, ABSENT_LINE_PREFIX, ABSENT_JAVA_LINE_PREFIX, EXIST_JS_ONLY_LINE_PREFIX);
            case JS:
                return processProposalAssertions(fileText, ABSENT_LINE_PREFIX, ABSENT_JS_LINE_PREFIX, EXIST_JAVA_ONLY_LINE_PREFIX);
        }

        throw new IllegalArgumentException("platform");
    }

    @NotNull
    public static CompletionProposal[] itemsShouldExist(String fileText) {
        return itemsShouldExist(fileText, Platform.ALL);
    }

    @NotNull
    public static CompletionProposal[] itemsShouldAbsent(String fileText) {
        return itemsShouldAbsent(fileText, Platform.ALL);
    }

    public static CompletionProposal[] processProposalAssertions(String fileText, String... prefixes) {
        Collection<CompletionProposal> proposals = new ArrayList<CompletionProposal>();
        for (String proposalStr : InTextDirectivesUtils.findListWithPrefixes(fileText, prefixes)) {
            Matcher matcher = CompletionProposal.PATTERN.matcher(proposalStr);
            matcher.find();
            proposals.add(new CompletionProposal(matcher.group(CompletionProposal.LOOKUP_STRING_GROUP_INDEX),
                                                 matcher.group(CompletionProposal.PRESENTABLE_STRING_GROUP_INDEX),
                                                 matcher.group(CompletionProposal.TAIL_TEXT_STRING_GROUP_INDEX)));
        }

        return ArrayUtil.toObjectArray(proposals, CompletionProposal.class);
    }

    @Nullable
    public static Integer getExpectedNumber(String fileText) {
        return getExpectedNumber(fileText, Platform.ALL);
    }

    @Nullable
    public static Integer getExpectedNumber(String fileText, Platform platform) {
        switch (platform) {
            case ALL:
                return InTextDirectivesUtils.getPrefixedInt(fileText, NUMBER_LINE_PREFIX);
            case JAVA:
                return getPlatformExpectedNumber(fileText, NUMBER_JAVA_LINE_PREFIX);
            case JS:
                return getPlatformExpectedNumber(fileText, NUMBER_JS_LINE_PREFIX);
        }

        throw new IllegalArgumentException("platform");
    }

    @Nullable
    public static Integer getExecutionTime(String fileText) {
        return InTextDirectivesUtils.getPrefixedInt(fileText, EXECUTION_TIME_PREFIX);
    }

    public static boolean isWithOrder(String fileText) {
        return InTextDirectivesUtils.getPrefixedInt(fileText, WITH_ORDER_PREFIX) != null;
    }

    public static void assertDirectivesValid(String fileText) {
        InTextDirectivesUtils.assertHasUnknownPrefixes(fileText, KNOWN_PREFIXES);
    }

    public static void assertContainsRenderedItems(CompletionProposal[] expected, LookupElement[] items, boolean checkOrder) {
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

    private static Integer getPlatformExpectedNumber(String fileText, String platformNumberPrefix) {
        Integer prefixedInt = InTextDirectivesUtils.getPrefixedInt(fileText, platformNumberPrefix);
        if (prefixedInt != null) {
            Assert.assertNull(String.format("There shouldn't be %s and %s prefixes set in same time", NUMBER_LINE_PREFIX,
                                            platformNumberPrefix),
                              InTextDirectivesUtils.getPrefixedInt(fileText, NUMBER_LINE_PREFIX));
            return prefixedInt;
        }

        return InTextDirectivesUtils.getPrefixedInt(fileText, NUMBER_LINE_PREFIX);
    }

    public static void assertNotContainsRenderedItems(CompletionProposal[] unexpected,LookupElement[] items) {
        List<CompletionProposal> itemsInformation = getItemsInformation(items);
        String allItemsString = listToString(itemsInformation);

        for (CompletionProposal unexpectedProposal : unexpected) {
            for (CompletionProposal proposal : itemsInformation) {
                Assert.assertFalse("Unexpected '" + unexpectedProposal + "' presented in " + allItemsString,
                                   proposal.isSuitable(unexpectedProposal));
            }
        }
    }

    public static List<CompletionProposal> getItemsInformation(LookupElement[] items) {
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

    public static String listToString(Collection<CompletionProposal> items) {
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
