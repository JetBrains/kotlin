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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupEx;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.ide.DataManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class LiveTemplatesTest extends JetLightCodeInsightFixtureTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(new File(PluginTestCaseBase.getTestDataPathBase(), "/templates").getPath() + File.separator);
        ((TemplateManagerImpl) TemplateManager.getInstance(getProject())).setTemplateTesting(true);
    }

    @Override
    protected void tearDown() throws Exception {
        ((TemplateManagerImpl) TemplateManager.getInstance(getProject())).setTemplateTesting(false);
        super.tearDown();
    }

    public void testSout() {
        paremeterless();
    }

    public void testSout_BeforeCall() {
        paremeterless();
    }

    public void testSout_BeforeCallSpace() {
        paremeterless();
    }

    public void testSout_BeforeBinary() {
        paremeterless();
    }

    public void testSout_InCallArguments() {
        paremeterless();
    }

    public void testSout_BeforeQualifiedCall() {
        paremeterless();
    }

    public void testSout_AfterSemicolon() {
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
        typeAndNextTab("y");

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

    public void testExfun() {
        start();

        typeAndNextTab("Int");
        typeAndNextTab("foo");
        typeAndNextTab("arg : Int");
        nextTab();

        checkAfter();
    }

    public void testExval() {
        start();

        typeAndNextTab("Int");
        nextTab();
        typeAndNextTab("Int");

        checkAfter();
    }

    public void testExvar() {
        start();

        typeAndNextTab("Int");
        nextTab();
        typeAndNextTab("Int");

        checkAfter();
    }

    public void testClosure() {
        start();

        typeAndNextTab("param");
        nextTab();

        checkAfter();
    }

    public void testInterface() {
        start();

        typeAndNextTab("SomeTrait");

        checkAfter();
    }

    public void testSingleton() {
        start();

        typeAndNextTab("MySingleton");

        checkAfter();
    }

    public void testVoid() {
        start();

        typeAndNextTab("foo");
        typeAndNextTab("x : Int");

        checkAfter();
    }

    public void testIter() {
        start();

        assertStringItems("args", "myList", "str", "stream");
        type("args");
        nextTab(2);

        checkAfter();
    }

    public void testAnonymous_1() {
        start();

        typeAndNextTab("Runnable");

        checkAfter();
    }

    public void testAnonymous_2() {
        start();

        typeAndNextTab("Thread");

        checkAfter();
    }

    private void doTestIfnInn() {
        start();

        assertStringItems("b", "t", "y");
        typeAndNextTab("b");

        checkAfter();
    }

    public void testIfn() {
        doTestIfnInn();
    }

    public void testInn() {
        doTestIfnInn();
    }

    private void paremeterless() {
        start();

        checkAfter();
    }

    private void start() {
        myFixture.configureByFile(getTestName(true) + ".kt");
        myFixture.type(getTemplateName());

        doAction("ExpandLiveTemplateByTab");
    }

    private String getTemplateName() {
        String testName = getTestName(true);
        if (testName.contains("_")) {
            return testName.substring(0, testName.indexOf("_"));
        }
        return testName;
    }

    private void checkAfter() {
        assertNull(getTemplateState());
        myFixture.checkResultByFile(getTestName(true) + ".exp.kt", true);
    }

    private void typeAndNextTab(String s) {
        type(s);
        nextTab();
    }

    private void type(String s) {
        myFixture.type(s);
    }

    private void nextTab() {
        UIUtil.invokeAndWaitIfNeeded(new Runnable() {
            @Override
            public void run() {
                CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            @Override
                            public void run() {
                                getTemplateState().nextTab();
                            }
                        });
                    }
                }, "nextTab", null);
            }
        });
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

    private void doAction(@NotNull String actionId) {
        EditorActionManager actionManager = EditorActionManager.getInstance();
        EditorActionHandler actionHandler = actionManager.getActionHandler(actionId);
        actionHandler.execute(myFixture.getEditor(), DataManager.getInstance().getDataContext(myFixture.getEditor().getComponent()));
    }

    private void assertStringItems(@NonNls String... items) {
        assertEquals(Arrays.asList(items), Arrays.asList(getItemStringsSorted()));
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
