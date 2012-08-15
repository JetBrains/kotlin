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

package org.jetbrains.jet.plugin.navigation;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.navigation.GotoImplementationHandler;
import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.testing.InTextDirectivesUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author Nikolay Krasko
 */
public class JetGotoImplementationTest extends LightCodeInsightTestCase {
    public void testClassNavigation() {
        doTest();
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/navigation/implementations").getPath() +
               File.separator;
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    protected void doTest() {
        configureByFile(getTestName(false) + ".kt");

        List<String> expectedReferences = Arrays.asList(
                InTextDirectivesUtils.findListWithPrefix("// REF:", getEditor().getDocument().getText()));

        GotoTargetHandler.GotoData elements = new GotoImplementationHandler().getSourceAndTargetElements(getEditor(), getFile());

        if (elements != null) {
            List<String> psiElements = Lists.transform(Arrays.asList(elements.targets), new Function<PsiElement, String>() {
                @Override
                public String apply(@Nullable PsiElement element) {
                    assertNotNull(element);
                    return element.toString();
                }
            });

            assertOrderedEquals(expectedReferences, psiElements);
        }
        else {
            assertEmpty(expectedReferences);
        }
    }
}
