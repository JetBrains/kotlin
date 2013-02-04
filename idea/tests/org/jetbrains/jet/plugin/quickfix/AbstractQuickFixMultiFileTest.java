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

import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import com.intellij.util.ui.UIUtil;
import junit.framework.ComparisonFailure;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractQuickFixMultiFileTest extends DaemonAnalyzerTestCase {

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

    public void doTest() throws Exception {
        configureByFiles(null, getTestFileNames().toArray(new String[1]));

        CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
            @Override
            public void run() {
                try {
                    final Pair<String, Boolean> pair = LightQuickFixTestCase.parseActionHint(getFile(), loadFile(getFile().getName()));
                    final String text = pair.getFirst();

                    final boolean actionShouldBeAvailable = pair.getSecond();

                    QuickFixActionsUtils.checkForUnexpectedErrors((JetFile) getFile());

                    doAction(text, actionShouldBeAvailable, getTestDataPath());
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
    public void doAction(final String text, final boolean actionShouldBeAvailable, final String testFullPath)
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
                QuickFixActionsUtils.checkAvailableActionsAreExpected((JetFile) getFile(), availableActions);
            }
        }
        else {
            if (!actionShouldBeAvailable) {
                fail("Action '" + text + "' is available (but must not) in test " + testFullPath);
            }

            ShowIntentionActionsHandler.chooseActionAndInvoke(getFile(), getEditor(), action, action.getText());

            UIUtil.dispatchAllInvocationEvents();

            if (!shouldBeAvailableAfterExecution()) {
                final IntentionAction afterAction = LightQuickFixTestCase.findActionWithText(getAvailableActions(), text);

                if (afterAction != null) {
                    fail("Action '" + text + "' is still available after its invocation in test " + testFullPath);
                }
            }

            checkResultByFile(getCheckFileName());
        }
    }

    protected abstract String getCheckFileName();

    protected abstract List<String> getTestFileNames();

    protected List<IntentionAction> getAvailableActions() {
        doHighlighting();
        return LightQuickFixTestCase.getAvailableActions(getEditor(), getFile());
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }
}
