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

package org.jetbrains.jet.editor.quickDoc;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.navigation.CtrlMouseHandler;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.ProjectDescriptorWithStdlibSources;

import java.util.List;

public abstract class AbstractJetQuickDocProviderTest extends LightCodeInsightFixtureTestCase {
    public void doTest(@NotNull String path) throws Exception {
        myFixture.configureByFile(path);

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
        assertNotNull("Can't find element at caret in file: " + path, element);

        DocumentationManager documentationManager = DocumentationManager.getInstance(myFixture.getProject());
        PsiElement targetElement = documentationManager.findTargetElement(myFixture.getEditor(), myFixture.getFile());

        List<String> directives = InTextDirectivesUtils.findLinesWithPrefixesRemoved(myFixture.getFile().getText(), "INFO:");
        assertTrue("Documentation to check should be added to test file with // INFO: directive " + path, 1 == directives.size());

        assertEquals(directives.get(0), CtrlMouseHandler.getInfo(targetElement, element));
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return ProjectDescriptorWithStdlibSources.INSTANCE;
    }
}
