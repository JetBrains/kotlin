package org.jetbrains.jet.codegen;

/**
 * @author yole
 * @author alex.tkachman
 */
public class StringsTest extends CodegenTestCase {

    public void testRawStrings() throws Exception {
        blackBoxFile("rawStrings.jet");
    }

    public void testKt881() throws Exception {
        blackBoxFile("regressions/kt881.jet");
    }
}
