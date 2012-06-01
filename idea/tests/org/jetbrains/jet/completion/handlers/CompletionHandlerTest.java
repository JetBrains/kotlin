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

package org.jetbrains.jet.completion.handlers;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.LightCompletionTestCase;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.projectRoots.Sdk;
import junit.framework.Assert;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay Krasko
 */
public class CompletionHandlerTest extends LightCompletionTestCase {

    public void testClassCompletionImport() {
        doTest(CompletionType.CLASS_NAME, 1, "SortedSet", null);
    }

    public void testNonStandardArray() {
        doTest(CompletionType.CLASS_NAME, 1, "Array", "java.lang.reflect");
    }

    public void testNoParamsFunction() {
        doTest();
    }

    public void testParamsFunction() {
        doTest();
    }

    public void testInsertJavaClassImport() {
        doTest();
    }

    public void testPropertiesSetter() {
        doTest();
    }

    public void testSingleBrackets() {
        configureByFile(getBeforeFileName());
        type('(');
        checkResultByFile(getAfterFileName());
    }

    public void testExistingSingleBrackets() {
        doTest();
    }

    public void testSureInsert() {
        doTest();
    }

    public void doTest() {
        doTest(CompletionType.BASIC, 2, null, null);
    }

    public void doTest(CompletionType type, int time, @Nullable String lookupString, @Nullable String tailText) {
        try {
            configureByFileNoComplete(getBeforeFileName());
            setType(type);

            complete(time);

            if (lookupString != null || tailText != null) {
                selectItem(getExistentLookupElement(lookupString, tailText), '\t');
            }

            checkResultByFile(getAfterFileName());
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static LookupElement getExistentLookupElement(@Nullable String lookupString, @Nullable String tailText) {
        final LookupImpl lookup = (LookupImpl) LookupManager.getInstance(getProject()).getActiveLookup();
        LookupElement foundElement = null;

        if (lookup != null) {
            final LookupElementPresentation presentation = new LookupElementPresentation();

            for (LookupElement lookupElement : lookup.getItems()) {
                boolean lookupOk;

                if (lookupString != null) {
                    lookupOk = (lookupElement.getLookupString().contains(lookupString));
                }
                else {
                    lookupOk = true;
                }

                boolean tailOk;

                if (tailText != null) {
                    lookupElement.renderElement(presentation);
                    String itemTailText = presentation.getTailText();
                    tailOk = itemTailText != null && itemTailText.contains(tailText);
                }
                else {
                    tailOk = true;
                }


                if (lookupOk && tailOk) {
                    if (foundElement != null) {
                        Assert.fail("Several elements satisfy to completion restrictions");
                    }
                    foundElement = lookupElement;
                }
            }
        }

        Assert.assertNotNull("No element found for given constraints",foundElement);
        return foundElement;
    }

    protected String getBeforeFileName() {
        return getTestName(false) + ".kt";
    }

    protected String getAfterFileName() {
        return getTestName(false) + ".kt.after";
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/handlers/").getPath() + File.separator;
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }
}
