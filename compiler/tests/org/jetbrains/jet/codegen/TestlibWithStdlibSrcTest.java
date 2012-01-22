package org.jetbrains.jet.codegen;

import junit.framework.TestSuite;

/**
 * @author Stepan Koltsov
 */
public class TestlibWithStdlibSrcTest extends TestlibTestBase {

    protected TestlibWithStdlibSrcTest() {
        super(false);
    }

    public static TestSuite suite() {
        return new TestlibWithStdlibSrcTest().buildSuite();
    }

}
