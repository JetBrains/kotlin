package org.jetbrains.jet.generators.injectors;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jetbrains.jet.di.DependencyInjectorGenerator;

import java.io.IOException;

@SuppressWarnings({"JUnitTestCaseWithNoTests", "JUnitTestCaseWithNonTrivialConstructors"})
public class GenerateInjectorsTest extends TestCase {

    private final DependencyInjectorGenerator generator;

    public GenerateInjectorsTest(DependencyInjectorGenerator generator) {
        super(generator.getInjectorClassName());
        this.generator = generator;
    }

    @Override
    protected void runTest() throws Throwable {
        CharSequence text = generator.generateText();
        String expected = FileUtil.loadFile(generator.getOutputFile(), true);
        String expectedText = StringUtil.convertLineSeparators(expected.trim());
        String actualText = StringUtil.convertLineSeparators(text.toString().trim());
        Assert.assertEquals("To fix this problem you need to run GenerateInjectors", expectedText, actualText);
    }

    public static TestSuite suite() throws IOException {
        TestSuite suite = new TestSuite();
        for (DependencyInjectorGenerator generator : GenerateInjectors.createGenerators()) {
            suite.addTest(new GenerateInjectorsTest(generator));
        }
        return suite;
    }
}
