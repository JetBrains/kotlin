package org.jetbrains.jet.codegen;

import junit.framework.TestSuite;

/**
 * @author Stepan Koltsov
 */
public class TestlibWithStdlibBinaryTest extends TestlibTestBase {

    protected TestlibWithStdlibBinaryTest() {
        super(true);
    }

    public static TestSuite suite() {
        return new TestlibWithStdlibBinaryTest().buildSuite();
    }

}
