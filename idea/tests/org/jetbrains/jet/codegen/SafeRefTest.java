package org.jetbrains.jet.codegen;

public class SafeRefTest extends CodegenTestCase {
    public void test247 () throws Exception {
        blackBoxFile("regressions/kt247.jet");
    }

    public void test245 () throws Exception {
        blackBoxFile("regressions/kt245.jet");
    }
}
