package org.jetbrains.jet.codegen;

/**
 * @author alex.tkachman
 */
public class FunctionGenTest extends CodegenTestCase {
    public void testDefaultArgs() throws Exception {
        blackBoxFile("functions/defaultargs.jet");
    }

    public void testNamedArgs() throws Exception {
        blackBoxFile("functions/defaultargs.jet");
    }

    public void testNoThisNoClosure() throws Exception {
        blackBoxFile("functions/nothisnoclosure.jet");
        System.out.println(generateToText());
    }
}
