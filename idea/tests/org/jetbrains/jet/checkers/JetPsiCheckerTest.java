package org.jetbrains.jet.checkers;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.PathManager;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;

/**
 * @author abreslav
 */
public class JetPsiCheckerTest extends LightDaemonAnalyzerTestCase {

    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/idea/testData";
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    public void testProperties() throws Exception {
        doTest("/checker/Properties.jet", true, true);
    }

    public void testBinaryCallsOnNullableValues() throws Exception {
        doTest("/checker/BinaryCallsOnNullableValues.jet", true, true);
    }

    public void testQualifiedThis() throws Exception {
        doTest("/checker/QualifiedThis.jet", true, true);
    }
}
