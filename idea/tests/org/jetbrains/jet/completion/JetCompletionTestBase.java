package org.jetbrains.jet.completion;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.LightCompletionTestCase;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikolay.Krasko
 */
public abstract class JetCompletionTestBase extends LightCompletionTestCase {

    private CompletionType type;

    protected void doTest() throws Exception {
        final String testName = getTestName(false);

        type = (testName.startsWith("Smart")) ? CompletionType.SMART : CompletionType.BASIC;

        configureByFile(testName + ".kt");

        assertContainsItems(itemsShouldExist(getFile().getText()));
        assertNotContainItems(itemsShouldAbsent(getFile().getText()));
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    @Override
    protected void complete(final int time) {
        new CodeCompletionHandlerBase(type, false, false, true).invokeCompletion(getProject(), getEditor(), time, false);

        LookupImpl lookup = (LookupImpl) LookupManager.getActiveLookup(myEditor);
        myItems = lookup == null ? null : lookup.getItems().toArray(LookupElement.EMPTY_ARRAY);
        myPrefix = lookup == null ? null : lookup.itemPattern(lookup.getItems().get(0));
    }

    @NotNull
    private static String[] itemsShouldExist(String fileText) {
        return findListWithPrefix("// EXIST:", fileText);
    }

    @NotNull
    private static String[] itemsShouldAbsent(String fileText) {
        return findListWithPrefix("// ABSENT:", fileText);
    }

    @NotNull
    private static String[] findListWithPrefix(String prefix, String fileText) {
        ArrayList<String> result = new ArrayList<String>();

        for (String line : findLinesWithPrefixRemoved(prefix, fileText)) {
            String[] completions = line.split(",");

            for (String completion : completions) {
                result.add(completion.trim());
            }
        }

        return result.toArray(new String[result.size()]);
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
