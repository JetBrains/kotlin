package org.jetbrains.jet.codegen;

public class SuperGenTest extends CodegenTestCase {
    public void testBasicProperty () {
        blackBoxFile("/super/basicproperty.jet");
//        System.out.println(generateToText());
    }

    public void testTraitProperty () {
        blackBoxFile("/super/traitproperty.jet");
//        System.out.println(generateToText());
    }

    public void testBasicMethodSuperTrait () {
        blackBoxFile("/super/basicmethodSuperTrait.jet");
//        System.out.println(generateToText());
    }

    public void testBasicMethodSuperClass () {
        blackBoxFile("/super/basicmethodSuperClass.jet");
//        System.out.println(generateToText());
    }

    public void testEnclosedMethod () {
        blackBoxFile("/super/enclosed.jet");
//        System.out.println(generateToText());
    }
}
