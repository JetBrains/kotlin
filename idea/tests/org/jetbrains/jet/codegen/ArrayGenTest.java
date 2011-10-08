package org.jetbrains.jet.codegen;

import jet.typeinfo.TypeInfo;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ArrayGenTest extends CodegenTestCase {
    public void testKt238 () throws Exception {
        blackBoxFile("regressions/kt238.jet");
    }

    public void testKt326 () throws Exception {
        blackBoxFile("regressions/kt326.jet");
        System.out.println(generateToText());
    }

    public void testCreateMultiInt () throws Exception {
        loadText("fun foo() = Array<Array<Int>> (5, { Array<Int>(it, {239}) })");
        Method foo = generateFunction();
        Integer[][] invoke = (Integer[][]) foo.invoke(null);
        assertEquals(invoke[2].length, 2);
        assertEquals(invoke[4].length, 4);
        assertEquals(invoke[4][2].intValue(), 239);
    }

    public void testCreateMultiIntNullable () throws Exception {
        loadText("fun foo() = Array<Array<Int?>> (5, { Array<Int?>(it) })");
        Method foo = generateFunction();
        Integer[][] invoke = (Integer[][]) foo.invoke(null);
        assertEquals(invoke[2].length, 2);
        assertEquals(invoke[4].length, 4);
    }

    public void testCreateMultiString () throws Exception {
        loadText("fun foo() = Array<Array<String>> (5, { Array<String>(0,{\"\"}) })");
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        System.out.println(invoke.getClass());
        assertTrue(invoke instanceof Object[]);
    }

    public void testCreateMultiGenerics () throws Exception {
        loadText("class L<T>() { val a = Array<T>(5) } fun foo() = L<Int>.a");
        System.out.println(generateToText());
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        System.out.println(invoke.getClass());
        assertTrue(invoke instanceof Integer[]);
    }

    public void testIntGenerics () throws Exception {
        loadText("class L<T>(var a : T) {} fun foo() = L<Int>(5).a");
        System.out.println(generateToText());
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        System.out.println(invoke.getClass());
        assertTrue(invoke instanceof Integer);
    }

    public void testIterator () throws Exception {
        loadText("fun box() { val x = Array<Int>(5, { it } ).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testPrimitiveIterator () throws Exception {
        loadText("fun box() { val x = ByteArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testLongIterator () throws Exception {
        loadText("fun box() { val x = LongArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testCharIterator () throws Exception {
        loadText("fun box() { val x = CharArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }
}
