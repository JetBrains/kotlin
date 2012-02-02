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

import java.io.File;

/**
 * @author Nikolay.Krasko
 */
public abstract class JetCompletionTestBase extends LightCompletionTestCase {
    private final String myPath;
    private final String myName;

    protected JetCompletionTestBase(@NotNull String path, @NotNull String name) {
        myPath = path;
        myName = name;

        // Set name explicitly because otherwise there will be "TestCase.fName cannot be null"
        setName("testCompletionExecute");
    }

    public void testCompletionExecute() throws Exception {
        doTest();
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), myPath).getPath() +
               File.separator;
    }

    @NotNull
    @Override
    public String getName() {
        return "test" + myName;
    }

    private CompletionType type;

    protected void doTest() throws Exception {
        final String testName = getTestName(false);

        type = (testName.startsWith("Smart")) ? CompletionType.SMART : CompletionType.BASIC;

        configureByFile(testName + ".kt");

        final String fileText = getFile().getText();
        final ExpectedCompletionUtils completionUtils = new ExpectedCompletionUtils();

        assertContainsItems(completionUtils.itemsShouldExist(fileText));
        assertNotContainItems(completionUtils.itemsShouldAbsent(fileText));
        
        Integer itemsNumber = completionUtils.getExpectedNumber(fileText);
        if (itemsNumber != null) {
            assertEquals(itemsNumber.intValue(), myItems.length);
        }
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
}
