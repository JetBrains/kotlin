package org.jetbrains.jet.codegen;

public class SuperGenTest extends CodegenTestCase {
    public void testBasicProperty () {
        blackBoxFile("/super/basicproperty.jet");
        System.out.println(generateToText());
    }

    public void testTraitProperty () {
        blackBoxFile("/super/traitproperty.jet");
        System.out.println(generateToText());
    }

    public void testBasicMethod () {
        blackBoxFile("/super/basicmethod.jet");
        System.out.println(generateToText());
    }
}
