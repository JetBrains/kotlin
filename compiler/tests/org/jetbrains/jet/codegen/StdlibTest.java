package org.jetbrains.jet.codegen;

/**
 * @author alex.tkachman
 */
public class StdlibTest extends CodegenTestCase {
    public void testInputStreamIterator () {
        blackBoxFile("inputStreamIterator.jet");
    }
}
