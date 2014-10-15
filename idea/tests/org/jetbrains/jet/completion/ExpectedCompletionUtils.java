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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.project.TargetPlatform;
import org.jetbrains.jet.plugin.stubs.AstAccessControl;
import org.junit.Assert;

import java.util.*;

/**
 * Extract a number of statements about completion from the given text. Those statements
 * should be asserted during test execution.
 */
public class ExpectedCompletionUtils {
    private ExpectedCompletionUtils() {
    }

    public static class CompletionProposal {
        public static final String LOOKUP_STRING = "lookupString";
        public static final String PRESENTATION_ITEM_TEXT = "itemText";
        public static final String PRESENTATION_TYPE_TEXT = "typeText";
        public static final String PRESENTATION_TAIL_TEXT = "tailText";
        public static final Set<String> validKeys = new HashSet<String>(
                Arrays.asList(LOOKUP_STRING, PRESENTATION_ITEM_TEXT, PRESENTATION_TYPE_TEXT, PRESENTATION_TAIL_TEXT)
        );

        private final Map<String, String> map;

        public CompletionProposal(@NotNull String lookupString) {
            map = new HashMap<String, String>();
            map.put(LOOKUP_STRING, lookupString);
        }

        public CompletionProposal(@NotNull Map<String, String> map) {
            this.map = map;
            for (String key : map.keySet()) {
                if (!validKeys.contains(key)){
                    throw new RuntimeException("Invalid key '" + key + "'");
                }
            }
        }

        public CompletionProposal(@NotNull JsonObject json) {
            map = new HashMap<String, String>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String key = entry.getKey();
                if (!validKeys.contains(key)) {
                    throw new RuntimeException("Invalid json property '" + key + "'");
                }
                map.put(key, entry.getValue().getAsString());
            }
        }

        public boolean matches(CompletionProposal expectedProposal) {
            for (Map.Entry<String, String> entry : expectedProposal.map.entrySet()) {
                if (!entry.getValue().equals(map.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            JsonObject jsonObject = new JsonObject();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                jsonObject.addProperty(entry.getKey(), entry.getValue());
            }
            return jsonObject.toString();
        }
    }

    private static final String UNSUPPORTED_PLATFORM_MESSAGE = String.format("Only %s and %s platforms are supported", TargetPlatform.JVM, TargetPlatform.JS);
    
    private static final String EXIST_LINE_PREFIX = "EXIST:";

    private static final String ABSENT_LINE_PREFIX = "ABSENT:";
    private static final String ABSENT_JS_LINE_PREFIX = "ABSENT_JS:";
    private static final String ABSENT_JAVA_LINE_PREFIX = "ABSENT_JAVA:";

    private static final String EXIST_JAVA_ONLY_LINE_PREFIX = "EXIST_JAVA_ONLY:";
    private static final String EXIST_JS_ONLY_LINE_PREFIX = "EXIST_JS_ONLY:";

    private static final String NUMBER_LINE_PREFIX = "NUMBER:";
    private static final String NUMBER_JS_LINE_PREFIX = "NUMBER_JS:";
    private static final String NUMBER_JAVA_LINE_PREFIX = "NUMBER_JAVA:";

    private static final String INVOCATION_COUNT_PREFIX = "INVOCATION_COUNT:";
    private static final String WITH_ORDER_PREFIX = "WITH_ORDER:";
    private static final String AUTOCOMPLETE_SETTING_PREFIX = "AUTOCOMPLETE_SETTING:";

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
            INVOCATION_COUNT_PREFIX,
            WITH_ORDER_PREFIX,
            AUTOCOMPLETE_SETTING_PREFIX,
            AstAccessControl.INSTANCE$.getALLOW_AST_ACCESS_DIRECTIVE());

    @NotNull
    public static CompletionProposal[] itemsShouldExist(String fileText, @Nullable TargetPlatform platform) {
        if (platform == null) {
            return processProposalAssertions(fileText, EXIST_LINE_PREFIX);
        }
        else if (platform == TargetPlatform.JVM) {
            return processProposalAssertions(fileText, EXIST_LINE_PREFIX, EXIST_JAVA_ONLY_LINE_PREFIX);
        }
        else if (platform == TargetPlatform.JS) {
            return processProposalAssertions(fileText, EXIST_LINE_PREFIX, EXIST_JS_ONLY_LINE_PREFIX);
        }
        else {
            throw new IllegalArgumentException(UNSUPPORTED_PLATFORM_MESSAGE);
        }
    }

    @NotNull
    public static CompletionProposal[] itemsShouldAbsent(String fileText, @Nullable TargetPlatform platform) {
        if (platform == null) {
            return processProposalAssertions(fileText, ABSENT_LINE_PREFIX);
        }
        else if (platform == TargetPlatform.JVM) {
            return processProposalAssertions(fileText, ABSENT_LINE_PREFIX, ABSENT_JAVA_LINE_PREFIX, EXIST_JS_ONLY_LINE_PREFIX);
        }
        else if (platform == TargetPlatform.JS) {
            return processProposalAssertions(fileText, ABSENT_LINE_PREFIX, ABSENT_JS_LINE_PREFIX, EXIST_JAVA_ONLY_LINE_PREFIX);
        }
        else {
            throw new IllegalArgumentException(UNSUPPORTED_PLATFORM_MESSAGE);
        }
    }

    public static CompletionProposal[] processProposalAssertions(String fileText, String... prefixes) {
        Collection<CompletionProposal> proposals = new ArrayList<CompletionProposal>();
        for (String proposalStr : InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, prefixes)) {
            if (proposalStr.startsWith("{")){
                JsonParser parser = new JsonParser();
                JsonElement json = parser.parse(proposalStr);
                proposals.add(new CompletionProposal((JsonObject) json));
            }
            else if (proposalStr.startsWith("\"") && proposalStr.endsWith("\"")) {
                proposals.add(new CompletionProposal(proposalStr.substring(1, proposalStr.length() - 1)));
            }
            else{
                for(String item : proposalStr.split(",")){
                    proposals.add(new CompletionProposal(item.trim()));
                }
            }
        }

        return ArrayUtil.toObjectArray(proposals, CompletionProposal.class);
    }

    @Nullable
    public static Integer getExpectedNumber(String fileText, @Nullable TargetPlatform platform) {
        if (platform == null) {
            return InTextDirectivesUtils.getPrefixedInt(fileText, NUMBER_LINE_PREFIX);
        }
        else if (platform == TargetPlatform.JVM) {
            return getPlatformExpectedNumber(fileText, NUMBER_JAVA_LINE_PREFIX);
        }
        else if (platform == TargetPlatform.JS) {
            return getPlatformExpectedNumber(fileText, NUMBER_JS_LINE_PREFIX);
        }
        else {
            throw new IllegalArgumentException(UNSUPPORTED_PLATFORM_MESSAGE);
        }
    }

    @Nullable
    public static Integer getInvocationCount(String fileText) {
        return InTextDirectivesUtils.getPrefixedInt(fileText, INVOCATION_COUNT_PREFIX);
    }

    @Nullable
    public static Boolean getAutocompleteSetting(String fileText) {
        return InTextDirectivesUtils.getPrefixedBoolean(fileText, AUTOCOMPLETE_SETTING_PREFIX);
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

                if (proposal.matches(expectedProposal)) {
                    isFound = true;

                    Assert.assertTrue("Invalid order of existent elements in " + allItemsString,
                                      !checkOrder || index > indexOfPrevious);
                    indexOfPrevious = index;

                    break;
                }
            }

            if (!isFound) {
                if (allItemsString.isEmpty()) {
                    Assert.fail("Completion is empty but " + expectedProposal + " is expected");
                }
                else {
                    Assert.fail("Expected " + expectedProposal + " not found in:\n" + allItemsString);
                }
            }
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

    public static void assertNotContainsRenderedItems(CompletionProposal[] unexpected, LookupElement[] items) {
        List<CompletionProposal> itemsInformation = getItemsInformation(items);
        String allItemsString = listToString(itemsInformation);

        for (CompletionProposal unexpectedProposal : unexpected) {
            for (CompletionProposal proposal : itemsInformation) {
                Assert.assertFalse("Unexpected '" + unexpectedProposal + "' presented in " + allItemsString,
                                   proposal.matches(unexpectedProposal));
            }
        }
    }

    public static List<CompletionProposal> getItemsInformation(LookupElement[] items) {
        LookupElementPresentation presentation = new LookupElementPresentation();

        List<CompletionProposal> result = new ArrayList<CompletionProposal>();
        if (items != null) {
            for (LookupElement item : items) {
                item.renderElement(presentation);
                Map<String, String> map = new HashMap<String, String>();
                map.put(CompletionProposal.LOOKUP_STRING, item.getLookupString());
                if (presentation.getItemText() != null){
                    map.put(CompletionProposal.PRESENTATION_ITEM_TEXT, presentation.getItemText());
                }
                if (presentation.getTypeText() != null){
                    map.put(CompletionProposal.PRESENTATION_TYPE_TEXT, presentation.getTypeText());
                }
                if (presentation.getTailText() != null){
                    map.put(CompletionProposal.PRESENTATION_TAIL_TEXT, presentation.getTailText());
                }
                result.add(new ExpectedCompletionUtils.CompletionProposal(map));
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
