package org.jetbrains.jet.codegen;

public class TupleGenTest extends CodegenTestCase {
    public void testBasic() {
        blackBoxFile("/tuples/basic.jet");
//        System.out.println(generateToText());
    }
}
