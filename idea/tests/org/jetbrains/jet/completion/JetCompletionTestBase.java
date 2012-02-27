/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.LightCompletionTestCase;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

/**
 * @author Nikolay.Krasko
 */
public abstract class JetCompletionTestBase extends LightCompletionTestCase {

    private final ExpectedCompletionUtils completionUtils = new ExpectedCompletionUtils();

    private CompletionType type;

    @Override
    protected abstract String getTestDataPath();

    protected void doTest() {
        try {
            final String testName = getTestName(false);

            type = (testName.startsWith("Smart")) ? CompletionType.SMART : CompletionType.BASIC;

            configureByFileNoComplete(testName + ".kt");

            final String fileText = getFile().getText();

            boolean withKotlinRuntime = ExpectedCompletionUtils.getPrefixedInt(fileText, "// RUNTIME:") != null;

            try {
                if (withKotlinRuntime) {
                    configureWithKotlinRuntime();
                }

                Integer completionTime = completionUtils.getExecutionTime(fileText);

                complete(completionTime == null ? 1 : completionTime);

                final String[] expected = completionUtils.itemsShouldExist(fileText);
                final String[] unexpected = completionUtils.itemsShouldAbsent(fileText);
                Integer itemsNumber = completionUtils.getExpectedNumber(fileText);

                assertTrue("Should be some assertions about completion", expected.length != 0 || unexpected.length != 0 || itemsNumber != null);

                assertContainsItems(expected);
                assertNotContainItems(unexpected);

                if (itemsNumber != null) {
                    assertEquals(itemsNumber.intValue(), myItems.length);
                }
            }
            finally {
                if (withKotlinRuntime) {
                    unConfigureKotlinRuntime();
                }
            }


        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    protected static void configureWithKotlinRuntime() {
        final ModuleRootManager rootManager = ModuleRootManager.getInstance(getModule());
        final ModifiableRootModel rootModel = rootManager.getModifiableModel();

        rootModel.setSdk(getFullJavaJDK());
        JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE.configureModule(getModule(), rootModel, null);

        rootModel.commit();
    }

    protected void unConfigureKotlinRuntime() {
        final ModuleRootManager rootManager = ModuleRootManager.getInstance(getModule());
        final ModifiableRootModel rootModel = rootManager.getModifiableModel();

        rootModel.setSdk(getProjectJDK());
        JetWithJdkAndRuntimeLightProjectDescriptor.unConfigureModule(rootModel);

        rootModel.commit();
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    protected static Sdk getFullJavaJDK() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }

    @Override
    protected void complete(final int time) {
        new CodeCompletionHandlerBase(type, false, false, true).invokeCompletion(getProject(), getEditor(), time, false);

        LookupImpl lookup = (LookupImpl) LookupManager.getActiveLookup(myEditor);
        myItems = lookup == null ? null : lookup.getItems().toArray(LookupElement.EMPTY_ARRAY);
        myPrefix = lookup == null ? null : lookup.itemPattern(lookup.getItems().get(0));
    }
}
