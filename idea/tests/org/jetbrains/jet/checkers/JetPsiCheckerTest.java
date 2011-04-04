package org.jetbrains.jet.checkers;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBase;

/**
 * @author abreslav
 */
public class JetPsiCheckerTest extends JetTestCaseBase {

    public JetPsiCheckerTest(String dataPath, String name) {
        super(dataPath, name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JetTestCaseBase.suiteForDirectory(getTestDataPathBase(), "/checker/", true, new JetTestCaseBase.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return new JetPsiCheckerTest(dataPath, name);
            }
        }));
        return suite;
    }
}
