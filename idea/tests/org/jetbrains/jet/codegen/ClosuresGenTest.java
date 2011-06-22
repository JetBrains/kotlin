package org.jetbrains.jet.codegen;

/**
 * @author max
 */
public class ClosuresGenTest extends CodegenTestCase {
    public void testSimplestClosure() throws Exception {
        blackBoxFile("classes/simplestClosure.jet");
    }

    public void testSimplestClosureAndBoxing() throws Exception {
        blackBoxFile("classes/simplestClosureAndBoxing.jet");
    }
}
