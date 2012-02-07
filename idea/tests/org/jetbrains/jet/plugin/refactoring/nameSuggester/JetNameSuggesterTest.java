package org.jetbrains.jet.plugin.refactoring.nameSuggester;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester;
import org.jetbrains.jet.plugin.refactoring.JetNameValidatorImpl;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringUtil;

import java.util.Arrays;

/**
 * User: Alefas
 * Date: 07.02.12
 */
public class JetNameSuggesterTest extends LightCodeInsightFixtureTestCase {
    public void testNameArrayOfClasses() {
        doTest();
    }

    public void testNameArrayOfStrings() {
        doTest();
    }

    public void testNameCallExpression() {
        doTest();
    }

    public void testNameClassCamelHump() {
        doTest();
    }

    public void testNameLong() {
        doTest();
    }

    public void testNameReferenceExpression() {
        doTest();
    }

    public void testNameString() {
        doTest();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/refactoring/nameSuggester");
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
        final String finalExpectedResultText = expectedResultText;
        try {
            JetRefactoringUtil.selectExpression(myFixture.getEditor(), file, new JetRefactoringUtil.SelectExpressionCallback() {
                @Override
                public void run(@Nullable JetExpression expression) {
                    String[] names = JetNameSuggester.suggestNames(expression, JetNameValidatorImpl.getEmptyValidator(getProject()));
                    Arrays.sort(names);
                    String result = StringUtil.join(names, "\n").trim();
                    assertEquals(finalExpectedResultText, result);
                }
            });
        } catch (JetRefactoringUtil.IntroduceRefactoringException e) {
            throw new AssertionError("Failed to find expression: " + e.getMessage());
        }

    }
}
