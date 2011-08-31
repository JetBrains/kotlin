package org.jetbrains.jet.codegen;

public class ArrayGetTestCase extends CodegenTestCase {
    public void testKt238 () throws Exception {
        blackBoxFile("regressions/kt238.jet");
    }
}
