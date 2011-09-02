package org.jetbrains.jet.codegen;

public class ArrayGenTestCase extends CodegenTestCase {
    public void testKt238 () throws Exception {
        blackBoxFile("regressions/kt238.jet");
    }
}
