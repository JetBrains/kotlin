package org.jetbrains.jet.codegen;

/**
 * @author yole
 */
public class PropertyGenTest extends CodegenTestCase {
    public void testPrivateVal() throws Exception {
        loadFile("privateVal.jet");
        System.out.println(generateToText());
        // TODO

    }
}
