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

package org.jetbrains.jet.plugin.parameterInfo;

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetValueArgumentList;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

/**
 * User: Alexander Podkhalyuzin
 * Date: 24.01.12
 */
public class JetFunctionParameterInfoTest extends LightCodeInsightFixtureTestCase {
    public void testInheritedFunctions() {
        doTest();
    }

    public void testInheritedWithCurrentFunctions() {
        doTest();
    }

    public void testNamedAndDefaultParameter() {
        doTest();
    }

    public void testNamedParameter() {
        doTest();
    }

    public void testNamedParameter2() {
        doTest();
    }

    public void testNotGreen() {
        doTest();
    }

    public void testNullableTypeCall() {
        doTest();
    }

    public void testPrintln() {
        doTest();
    }

    public void testSimple() {
        doTest();
    }

    public void testSimpleConstructor() {
        doTest();
    }

    public void testSuperConstructorCall() {
        doTest();
    }

    public void testTwoFunctions() {
        doTest();
    }

    public void testTwoFunctionsGrey() {
        doTest();
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/parameterInfo/functionParameterInfo");
    }

    private void doTest() {
        myFixture.configureByFile(getTestName(false) + ".kt");
        JetFile file = (JetFile) myFixture.getFile();
        PsiElement lastChild = file.getLastChild();
        assert lastChild != null;
        String expectedResultText = null;
        if (lastChild.getNode().getElementType().equals(JetTokens.BLOCK_COMMENT)) {
            String lastChildText = lastChild.getText();
            expectedResultText = lastChildText.substring(2, lastChildText.length() - 2).trim();
        }
        else if (lastChild.getNode().getElementType().equals(JetTokens.EOL_COMMENT)) {
            expectedResultText = lastChild.getText().substring(2).trim();
        }
        assert expectedResultText != null;
        JetFunctionParameterInfoHandler parameterInfoHandler = new JetFunctionParameterInfoHandler();
        MockCreateParameterInfoContext mockCreateParameterInfoContext = new MockCreateParameterInfoContext(file, myFixture);
        JetValueArgumentList parameterOwner = parameterInfoHandler.findElementForParameterInfo(mockCreateParameterInfoContext);

        MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(file, myFixture);

        //to update current parameter index
        parameterInfoHandler.findElementForUpdatingParameterInfo(updateContext);

        MockParameterInfoUIContext parameterInfoUIContext =
                new MockParameterInfoUIContext(parameterOwner, updateContext.getCurrentParameter());

        for (Object item : mockCreateParameterInfoContext.getItemsToShow()) {
            parameterInfoHandler.updateUI(item, parameterInfoUIContext);
        }
        assertEquals(expectedResultText, parameterInfoUIContext.getResultText());
    }
}
