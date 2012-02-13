package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupEx;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.ide.DataManager;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Evgeny Gerashchenko
 * @since 2/10/12
 */
public class LiveTemplatesTest extends LightCodeInsightFixtureTestCase {
    public void testSout() {
        paremeterless();
    }

    public void testSerr() {
        paremeterless();
    }

    public void testMain() {
        paremeterless();
    }

    public void testSoutv() {
        start();
        
        assertStringItems("args", "x", "y");
        type("y");
        nextTab();

        checkAfter();
    }

    public void testSoutp() {
        paremeterless();
    }

    public void testFun0() {
        start();

        type("foo");
        nextTab(2);

        checkAfter();
    }

    public void testFun1() {
        start();

        type("foo");
        nextTab(4);

        checkAfter();
    }

    public void testFun2() {
        start();

        type("foo");
        nextTab(6);

        checkAfter();
    }

    public void testIter() {
        start();

        assertStringItems("args", "collection", "myList", "str", "stream");
        type("args");
        nextTab(2);

        checkAfter();
    }



    private void paremeterless() {
        start();

        checkAfter();
    }

    private void start() {
        myFixture.configureByFile(getTestName(true) + ".kt");
        myFixture.type(getTestName(true));

        doAction("ExpandLiveTemplateByTab");
    }

    private void checkAfter() {
        assertNull(getTemplateState());
        myFixture.checkResultByFile(getTestName(true) + ".exp.kt");
    }

    private void type(String s) {
        myFixture.type(s);
    }

    private void nextTab() {
        getTemplateState().nextTab();
    }

    private void nextTab(int times) {
        for (int i = 0; i < times; i++) {
            nextTab();
        }
    }

    private TemplateState getTemplateState() {
        return TemplateManagerImpl.getTemplateState(myFixture.getEditor());
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }

    @SuppressWarnings("MethodOverridesPrivateMethodOfSuperclass")
    private void doAction(@NotNull String actionId) {
        EditorActionManager actionManager = EditorActionManager.getInstance();
        EditorActionHandler actionHandler = actionManager.getActionHandler(actionId);
        actionHandler.execute(myFixture.getEditor(), DataManager.getInstance().getDataContext());
    }

    @Override
    protected void tearDown() throws Exception {
        ((TemplateManagerImpl) TemplateManager.getInstance(getProject())).setTemplateTesting(false);
        super.tearDown();
    }

    private void assertStringItems(@NonNls String... items) {
        assertEquals(Arrays.asList(items), Arrays.asList(getItemStringsSorted()));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(new File(PluginTestCaseBase.getTestDataPathBase(), "/templates").getPath() +
                                  File.separator);
        ((TemplateManagerImpl) TemplateManager.getInstance(getProject())).setTemplateTesting(true);
    }

    private String[] getItemStrings() {
        LookupEx lookup = LookupManager.getActiveLookup(myFixture.getEditor());
        assertNotNull(lookup);
        ArrayList<String> result = new ArrayList<String>();
        for (LookupElement element : lookup.getItems()) {
            result.add(element.getLookupString());
        }
        return ArrayUtil.toStringArray(result);
    }
    
    private String[] getItemStringsSorted() {
        String[] items = getItemStrings();
        Arrays.sort(items);
        return items;
    }
}
