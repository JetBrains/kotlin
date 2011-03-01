package org.jetbrains.jet.resolve;

import com.intellij.openapi.application.PathManager;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;

/**
 * @author abreslav
 */
public class JetResolveTest extends ExtensibleResolveTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/idea/testData";
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    public void testBasic() throws Exception {
        doTest("/resolve/Basic.jet", true, true);
    }

}