package org.jetbrains.jet.confluence.rendering;

import com.atlassian.renderer.v2.macro.MacroException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jetbrains.jet.confluence.TestUtils;
import org.jetbrains.jet.lexer.JetMacro;

import java.io.File;
import java.io.IOException;

/**
 * @author Natalia.Ukhorskaya
 */

public class ConfluenceRenderingTest extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        TestSuite ats = new TestSuite();
        suite.addTest(ats);
        File parsingSourceDir = new File("./testData/rendering");
        addFilesFromDirToSuite(parsingSourceDir, ats);
        return suite;
    }

    private static void addFilesFromDirToSuite(File file, TestSuite ats) {
        if (file.isDirectory()) {
            for (File sourceFile : file.listFiles()) {
                 addFilesFromDirToSuite(sourceFile, ats);
            }
        }   else {
            if (file.getName().endsWith(".kt")) {
                ats.addTest(new ConfluenceRenderingTest(file.getName(), file));
            }
        }
    }

    private final File sourceFile;

    public ConfluenceRenderingTest(String name, File sourceFile) {
        super(name);
        this.sourceFile = sourceFile;
    }

    @Override
    protected void runTest() throws Throwable {
        checkSourceFile(sourceFile);
    }

    private void checkSourceFile(File sourceFile) throws IOException, MacroException {
        String source = TestUtils.readFile(sourceFile);

        StringBuilder actualResult = new StringBuilder();
        new JetMacro().generateHtmlFromCode(source, actualResult);

        File expectedResultFile = new File(sourceFile.getAbsolutePath().replace(".kt", ".txt"));
        if (!expectedResultFile.exists()) {
            expectedResultFile.createNewFile();
            TestUtils.writeFile(expectedResultFile, TestUtils.divideResultForLines(actualResult.toString()));
            assertTrue("File with expected result for " + expectedResultFile.getAbsolutePath() + " is absent.", false);
        }

        String expectedResult = TestUtils.readFile(expectedResultFile);


        assertEquals(expectedResult,
                TestUtils.divideResultForLines(actualResult.toString()));
    }
}
