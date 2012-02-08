package org.jetbrains.jet.plugin.refactoring.introduceVariable;

import com.intellij.ide.DataManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

/**
 * User: Alefas
 * Date: 31.01.12
 */
public class JetIntroduceVariableTest extends LightCodeInsightFixtureTestCase {
    public void testClassBody() {
        doTest();
    }

    public void testDoWhileAddBlock() {
        doTest();
    }

    public void testFewOccurrences() {
        doTest();
    }

    public void testFunctionAddBlock() {
        doTest();
    }

    public void testIfCondition() {
        doTest();
    }

    public void testIfElseAddBlock() {
        doTest();
    }

    public void testIfThenAddBlock() {
        doTest();
    }

    public void testIt() {
        doTest();
    }

    /*public void testLoopRange() {
        doTest();
    }*/

    public void testManyInnerOccurences() {
        doTest();
    }

    public void testManyOccurrences() {
        doTest();
    }

    public void testReplaceOccurence() {
        doTest();
    }

    public void testSimple() {
        doTest();
    }

    public void testSimpleCreateValue() {
        doTest();
    }

    public void testStringInjection() {
        doTest();
    }

    public void testWhenAddBlock() {
        doTest();
    }

    public void testWhenParts() {
        doTest();
    }

    public void testWhileAddBlock() {
        doTest();
    }

    public void testWhileCondition() {
        doTest();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/refactoring/introduceVariable");
    }

    private void doTest() {
        myFixture.configureByFile(getTestName(false) + ".kt");
        final JetFile file = (JetFile) myFixture.getFile();
        PsiElement lastChild = file.getLastChild();
        assert lastChild != null;
        String expectedResultText = null;
        if (lastChild.getNode().getElementType().equals(JetTokens.BLOCK_COMMENT)) {
            String lastChildText = lastChild.getText();
            expectedResultText = lastChildText.substring(2, lastChildText.length() - 2).trim();
        } else if (lastChild.getNode().getElementType().equals(JetTokens.EOL_COMMENT)) {
            expectedResultText = lastChild.getText().substring(2).trim();
        }
        assert expectedResultText != null;
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                new JetIntroduceVariableHandler().invoke(getProject(), myFixture.getEditor(), file,
                                                         DataManager.getInstance().getDataContext(myFixture.getEditor().
                                                                 getComponent()));
            }
        });
        int endOffset = file.getLastChild().getTextRange().getStartOffset();
        assertEquals(expectedResultText, file.getText().substring(0, endOffset).trim());
    }
}
