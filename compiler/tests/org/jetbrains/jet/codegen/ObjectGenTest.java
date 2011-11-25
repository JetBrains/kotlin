package org.jetbrains.jet.codegen;

/**
 * @author yole
 * @author alex.tkachman
 */
public class ObjectGenTest extends CodegenTestCase {
    public void testSimpleObject() throws Exception {
        blackBoxFile("objects/simpleObject.jet");
    }

    public void testObjectLiteral() throws Exception {
        blackBoxFile("objects/objectLiteral.jet");
//        System.out.println(generateToText());
    }

    public void testMethodOnObject() throws Exception {
        blackBoxFile("objects/methodOnObject.jet");
    }

    public void testKt535() throws Exception {
        blackBoxFile("regressions/kt535.jet");
    }

    public void testKt560() throws Exception {
        blackBoxFile("regressions/kt560.jet");
    }
}
