package org.jetbrains.jet.codegen;

public class TraitsTest  extends CodegenTestCase {
    @Override
    protected String getPrefix() {
        return "traits";
    }

    public void testSimple () throws Exception {
        blackBoxFile("traits/simple.jet");
        System.out.println(generateToText());
    }

    public void testWithRequired () throws Exception {
        blackBoxFile("traits/withRequired.jet");
        System.out.println(generateToText());
    }

    public void testMultiple () throws Exception {
        blackBoxFile("traits/multiple.jet");
        System.out.println(generateToText());
    }
}
