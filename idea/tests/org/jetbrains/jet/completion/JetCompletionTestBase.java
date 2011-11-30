package org.jetbrains.jet.completion;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupEx;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Nikolay.Krasko
 */
public abstract class JetCompletionTestBase extends LightCodeInsightTestCase {

    protected void doTest() {
        final String testName = getTestName(false);
        configureByFile(testName + ".kt");

        CompletionType completionType = (testName.startsWith("Smart")) ? CompletionType.SMART : CompletionType.BASIC;
        new CodeCompletionHandlerBase(completionType, false, false, true).invokeCompletion(getProject(), getEditor());

        LookupEx lookup = LookupManager.getActiveLookup(getEditor());
        assert lookup != null;

        HashSet<String> items = new HashSet<String>(resolveLookups(lookup.getItems()));

        List<String> shouldExist = itemsShouldExist(getFile().getText());
        for (String shouldExistItem : shouldExist) {
            assertTrue(String.format("Should contain proposal '%s'.", shouldExistItem),
                       items.contains(shouldExistItem));
        }
        
        List<String> shouldAbsent = itemsShouldAbsent(getFile().getText());
        for (String shouldAbsentItem : shouldAbsent) {
            assertTrue(String.format("Shouldn't contain proposal '%s'.", shouldAbsentItem),
                       !items.contains(shouldAbsentItem));
        }
    }
    
    private static List<String> resolveLookups(List<LookupElement> items) {
        ArrayList<String> result = new ArrayList<String>(items.size());
        for (LookupElement item : items) {
            result.add(item.getLookupString());
        }

        return result;
    }

    @NotNull
    private static List<String> itemsShouldExist(String fileText) {
        return findListWithPrefix("// EXIST:", fileText);
    }

    @NotNull
    private static List<String> itemsShouldAbsent(String fileText) {
        return findListWithPrefix("// ABSENT:", fileText);
    }

    @NotNull
    private static List<String> findListWithPrefix(String prefix, String fileText) {
        ArrayList<String> result = new ArrayList<String>();

        for (String line : findLinesWithPrefixRemoved(prefix, fileText)) {
            String[] completions = line.split(",");

            for (String completion : completions) {
                result.add(completion.trim());
            }
        }

        return result;
    }

    @NotNull
    private static List<String> findLinesWithPrefixRemoved(String prefix, String fileText) {
        ArrayList<String> result = new ArrayList<String>();

        for (String line : fileNonEmptyLines(fileText)) {
            if (line.startsWith(prefix)) {
                result.add(line.substring(prefix.length()).trim());
            }
        }

        return result;
    }

    @NotNull
    private static List<String> fileNonEmptyLines(String fileText) {
        ArrayList<String> result = new ArrayList<String>();

        BufferedReader reader = new BufferedReader(new StringReader(fileText));

        try {
            String line;

            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    result.add(line.trim());
                }
            }
        } catch(IOException e) {
            assert false;
        }

        return result;
    }
}
