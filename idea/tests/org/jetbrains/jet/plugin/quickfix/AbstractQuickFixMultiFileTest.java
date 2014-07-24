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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ui.UIUtil;
import junit.framework.ComparisonFailure;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.DirectiveBasedActionUtils;
import org.jetbrains.jet.plugin.KotlinDaemonAnalyzerTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractQuickFixMultiFileTest extends KotlinDaemonAnalyzerTestCase {

    protected static boolean shouldBeAvailableAfterExecution() {
        return false;
    }

    private static List<String> getActionsTexts(List<IntentionAction> availableActions) {
        List<String> texts = new ArrayList<String>();
        for (IntentionAction intentionAction : availableActions) {
            texts.add(intentionAction.getText());
        }
        return texts;
    }

    protected void doTestWithoutExtraFile(String beforeFileName) throws Exception {
        doTest(beforeFileName, false);
    }

    protected void doTestWithExtraFile(String beforeFileName) throws Exception {
        doTest(beforeFileName, true);
    }

    private void doTest(final String beforeFileName, boolean withExtraFile) throws Exception {
        if (withExtraFile) {
            configureByFiles(null, beforeFileName, beforeFileName.replace(".Main.", ".data.Sample."));
        }
        else {
            configureByFiles(null, beforeFileName);
        }

        CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
            @Override
            public void run() {
                try {
                    Pair<String, Boolean> pair = LightQuickFixTestCase.parseActionHint(
                            getFile(),FileUtil.loadFile(new File(getTestDataPath() + beforeFileName), true));
                    String text = pair.getFirst();

                    boolean actionShouldBeAvailable = pair.getSecond();

                    DirectiveBasedActionUtils.checkForUnexpectedErrors((JetFile) getFile());

                    doAction(text, actionShouldBeAvailable, beforeFileName);
                }
                catch (ComparisonFailure e) {
                    throw e;
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    fail(getTestName(true));
                }
            }
        }, "", "");
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public void doAction(String text, boolean actionShouldBeAvailable, String testFullPath)
            throws Exception {
        List<IntentionAction> availableActions = getAvailableActions();
        IntentionAction action = LightQuickFixTestCase.findActionWithText(availableActions, text);

        if (action == null) {
            if (actionShouldBeAvailable) {
                List<String> texts = getActionsTexts(availableActions);
                Collection<HighlightInfo> infos = doHighlighting();
                fail("Action with text '" + text + "' is not available in test " + testFullPath + "\n" +
                     "Available actions (" + texts.size() + "): " + texts + "\n" +
                     availableActions + "\n" +
                     "Infos:" + infos);
            }
            else {
                DirectiveBasedActionUtils.checkAvailableActionsAreExpected((JetFile) getFile(), availableActions);
            }
        }
        else {
            if (!actionShouldBeAvailable) {
                fail("Action '" + text + "' is available (but must not) in test " + testFullPath);
            }

            ShowIntentionActionsHandler.chooseActionAndInvoke(getFile(), getEditor(), action, action.getText());

            UIUtil.dispatchAllInvocationEvents();

            if (!shouldBeAvailableAfterExecution()) {
                IntentionAction afterAction = LightQuickFixTestCase.findActionWithText(getAvailableActions(), text);

                if (afterAction != null) {
                    fail("Action '" + text + "' is still available after its invocation in test " + testFullPath);
                }
            }

            checkResultByFile(testFullPath.replace(".before.Main.", ".after."));
        }
    }

    private List<IntentionAction> getAvailableActions() {
        doHighlighting();
        return LightQuickFixTestCase.getAvailableActions(getEditor(), getFile());
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBuilder.getHomeDirectory() + "/";
    }
}
