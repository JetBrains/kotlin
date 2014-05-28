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

import com.intellij.psi.*;
import com.intellij.psi.impl.JavaCodeBlockModificationListener;
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

public abstract class AbstractOutOfBlockModificationTest extends JetLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/outOfBlock");
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/outOfBlock";
    }

    protected void doTest(String path) {
        myFixture.configureByFile(path);
        String text = myFixture.getDocument(myFixture.getFile()).getText();

        boolean expectedOutOfBlock = false;
        if (text.startsWith("// TRUE")) {
            expectedOutOfBlock = true;
        }
        else if (text.startsWith("// FALSE")) {
            expectedOutOfBlock = false;
        }
        else {
            fail("Expectation of code block result test should be configured with " +
                 "\"// TRUE\" or \"// FALSE\" directive in the beginning of the file");
        }

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull("Should be valid element", element);

        assertEquals("Result for out of block test is differs from expected on element " + element,
                     !expectedOutOfBlock, isInsideCodeBlock(element));
    }

    /**
     * Copy of private {@link JavaCodeBlockModificationListener.isInsideCodeBlock()}
     */
    @SuppressWarnings("JavadocReference")
    private static boolean isInsideCodeBlock(PsiElement element) {
        if (element instanceof PsiFileSystemItem) {
            return false;
        }

        if (element == null || element.getParent() == null) return true;

        PsiElement parent = element;
        while (true) {
            if (parent instanceof PsiFile || parent instanceof PsiDirectory || parent == null) {
                return false;
            }
            if (parent instanceof PsiClass) return false; // anonymous or local class
            if (parent instanceof PsiModifiableCodeBlock) {
                if (!((PsiModifiableCodeBlock)parent).shouldChangeModificationCount(element)) {
                    return true;
                }
            }
            parent = parent.getParent();
        }
    }
}
