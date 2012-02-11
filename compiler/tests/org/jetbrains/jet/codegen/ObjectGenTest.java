package org.jetbrains.jet.codegen;

/**
 * @author yole
 * @author alex.tkachman
 */
public class ObjectGenTest extends CodegenTestCase {
    public void testSimpleObject() throws Exception {
        blackBoxFile("objects/simpleObject.jet");
//        System.out.println(generateToText());
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

    public void testKt640() throws Exception {
        blackBoxFile("regressions/kt640.jet");
    }

    public void testKt1136() throws Exception {
        blackBoxFile("regressions/kt1136.kt");
    }

    public void testKt1047() throws Exception {
        blackBoxFile("regressions/kt1047.kt");
    }

    public void testKt694() throws Exception {
        blackBoxFile("regressions/kt694.kt");
    }

    public void testKt1186() throws Exception {
        blackBoxFile("regressions/kt1186.kt");
    }
}
