package org.jetbrains.jet.codegen;

import java.lang.reflect.Method;

public class ArrayGenTest extends CodegenTestCase {
    public void testKt238 () throws Exception {
        blackBoxFile("regressions/kt238.jet");
    }

    public void testKt326 () throws Exception {
        blackBoxFile("regressions/kt326.jet");
    }

    public void testCreateMultiInt () throws Exception {
        loadText("fun foo() = Array<Array<Int>> (5)");
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        System.out.println(invoke.getClass());
        assertTrue(invoke instanceof int[][]);
    }

    public void testCreateMultiString () throws Exception {
        loadText("fun foo() = Array<Array<String>> (5)");
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        System.out.println(invoke.getClass());
        assertTrue(invoke instanceof String[][]);
    }

    public void testCreateMultiGenerics () throws Exception {
        /*
        loadText("class L<T>() { val a = Array<T>(5) } fun foo() = L<Int>.a");
        System.out.println(generateToText());
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        System.out.println(invoke.getClass());
        assertTrue(invoke instanceof Integer[]);
        */
    }

    public void testIntGenerics () throws Exception {
        loadText("class L<T>(var a : T) {} fun foo() = L<Int>(5).a");
        System.out.println(generateToText());
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        System.out.println(invoke.getClass());
        assertTrue(invoke instanceof Integer);
    }
}
