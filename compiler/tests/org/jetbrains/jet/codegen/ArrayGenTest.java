package org.jetbrains.jet.codegen;

import java.lang.reflect.Method;

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
        loadText("class L<T>() { val a = Array<T?>(5) } fun foo() = L<Int>.a");
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
        loadText("fun box() { val x = Array<Int>(5, { it } ).iterator(); while(x.hasNext) { java.lang.System.out?.println(x.next()) } }");
        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testPrimitiveIterator () throws Exception {
        loadText("fun box() { val x = ByteArray(5).iterator(); while(x.hasNext) { java.lang.System.out?.println(x.next()) } }");
        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testLongIterator () throws Exception {
        loadText("fun box() { val x = LongArray(5).iterator(); while(x.hasNext) { java.lang.System.out?.println(x.next()) } }");
        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testCharIterator () throws Exception {
        loadText("fun box() { val x = CharArray(5).iterator(); while(x.hasNext) { java.lang.System.out?.println(x.next()) } }");
        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testArrayIndices () throws Exception {
        loadText("fun box() { val x = Array<Int>(5, {it}).indices.iterator(); while(x.hasNext) { java.lang.System.out?.println(x.next()) } }");
        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testCharIndices () throws Exception {
        loadText("fun box() { val x = CharArray(5).indices.iterator(); while(x.hasNext) { java.lang.System.out?.println(x.next()) } }");
        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testCollectionPlusAssign () throws Exception {
        blackBoxFile("regressions/kt33.jet");
    }

    public void testArrayPlusAssign () throws Exception {
        loadText("fun box() : Int { val s = IntArray(1); s [0] = 5; s[0] += 7; return s[0] }");
        System.out.println(generateToText());
        Method foo = generateFunction();
        assertTrue((Integer)foo.invoke(null) == 12);
    }

    public void testCollectionAssignGetMultiIndex () throws Exception {
            loadText("import java.util.ArrayList\n" +
                         "fun box() : String { val s = ArrayList<String>(1); s.add(\"\"); s [1, -1] = \"5\"; s[2, -2] += \"7\"; return s[2,-2] }\n" +
                         "fun ArrayList<String>.get(index1: Int, index2 : Int) = this[index1+index2]\n" +
                         "fun ArrayList<String>.set(index1: Int, index2 : Int, elem: String) { this[index1+index2] = elem }\n");
            System.out.println(generateToText());
            Method foo = generateFunction("box");
        assertTrue(foo.invoke(null).equals("57"));
        }

    public void testArrayGetAssignMultiIndex () throws Exception {
        loadText(
                     "fun box() : String? { val s = Array<String>(1,{ \"\" }); s [1, -1] = \"5\"; s[2, -2] += \"7\"; return s[-3,3] }\n" +
                     "fun Array<String>.get(index1: Int, index2 : Int) = this[index1+index2]\n" +
                     "fun Array<String>.set(index1: Int, index2 : Int, elem: String) { this[index1+index2] = elem\n }");
        System.out.println(generateToText());
        Method foo = generateFunction("box");
        assertTrue(foo.invoke(null).equals("57"));
    }

    public void testCollectionGetMultiIndex () throws Exception {
            loadText("import java.util.ArrayList\n" +
                         "fun box() : String { val s = ArrayList<String>(1); s.add(\"\"); s [1, -1] = \"5\"; return s[2, -2] }\n" +
                         "fun ArrayList<String>.get(index1: Int, index2 : Int) = this[index1+index2]\n" +
                         "fun ArrayList<String>.set(index1: Int, index2 : Int, elem: String) { this[index1+index2] = elem }\n");
            System.out.println(generateToText());
            Method foo = generateFunction("box");
            assertTrue(foo.invoke(null).equals("5"));
        }

    public void testArrayGetMultiIndex () throws Exception {
        loadText(
                     "fun box() : String? { val s = Array<String>(1,{ \"\" }); s [1, -1] = \"5\"; return s[-2, 2] }\n" +
                     "fun Array<String>.get(index1: Int, index2 : Int) = this[index1+index2]\n" +
                     "fun Array<String>.set(index1: Int, index2 : Int, elem: String) { this[index1+index2] = elem\n }");
        System.out.println(generateToText());
        Method foo = generateFunction("box");
        assertTrue(foo.invoke(null).equals("5"));
    }

    public void testMap () throws Exception {
        loadText(
                     "fun box() : Int? { val s = java.util.HashMap<String,Int?>(); s[\"239\"] = 239; return s[\"239\"] }\n" +
                     "fun java.util.HashMap<String,Int?>.set(index: String, elem: Int?) { this.put(index, elem) }");
        System.out.println(generateToText());
        Method foo = generateFunction("box");
        assertTrue((Integer)foo.invoke(null) == 239);
    }

    public void testLongDouble () throws Exception {
        loadText(
                     "fun box() : Int { var l = IntArray(1); l[0.lng] = 4; l[0.lng] += 6; return l[0.lng];}\n" +
                     "fun IntArray.set(index: Long, elem: Int) { this[index.int] = elem }\n" +
                     "fun IntArray.get(index: Long) = this[index.int]");
        System.out.println(generateToText());
        Method foo = generateFunction("box");
        assertTrue((Integer)foo.invoke(null) == 10);
    }
}
