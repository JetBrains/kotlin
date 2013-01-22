/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.codegen;

import org.jetbrains.jet.ConfigurationKind;

import java.lang.reflect.Method;

public class ArrayGenTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testKt238() throws Exception {
        blackBoxFile("regressions/kt238.kt");
    }

    public void testKt326() throws Exception {
        //        blackBoxFile("regressions/kt326.kt");
        //        System.out.println(generateToText());
    }

    public void testKt779() throws Exception {
        blackBoxFile("regressions/kt779.kt");
        //        System.out.println(generateToText());
    }

    public void testCreateMultiInt() throws Exception {
        loadText("fun foo() = Array<Array<Int>> (5, { Array<Int>(it, {239}) })");
        Method foo = generateFunction();
        try {
            Integer[][] invoke = (Integer[][]) foo.invoke(null);
            assertEquals(invoke[2].length, 2);
            assertEquals(invoke[4].length, 4);
            assertEquals(invoke[4][2].intValue(), 239);
        }
        catch (Throwable e) {
            System.out.println(generateToText());
            throw ((e instanceof RuntimeException)) ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    public void testCreateMultiIntNullable() throws Exception {
        loadText("fun foo() = Array<Array<Int?>> (5, { arrayOfNulls<Int>(it) })");
        Method foo = generateFunction();
        Integer[][] invoke = (Integer[][]) foo.invoke(null);
        assertEquals(invoke[2].length, 2);
        assertEquals(invoke[4].length, 4);
    }

    public void testCreateMultiString() throws Exception {
        loadText("fun foo() = Array<Array<String>> (5, { Array<String>(0,{\"\"}) })");
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        System.out.println(invoke.getClass());
        assertTrue(invoke instanceof Object[]);
    }

    public void testCreateMultiGenerics() throws Exception {
        //        loadText("class L<T>() { val a = Array<T?>(5) } fun foo() = L<Int>.a");
        //        System.out.println(generateToText());
        //        Method foo = generateFunction();
        //        Object invoke = foo.invoke(null);
        //        System.out.println(invoke.getClass());
        //        assertTrue(invoke.getClass() == Object[].class);
    }

    public void testIntGenerics() throws Exception {
        loadText("class L<T>(var a : T) {} fun foo() = L<Int>(5).a");
        //System.out.println(generateToText());
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        System.out.println(invoke.getClass());
        assertTrue(invoke instanceof Integer);
    }

    public void testIterator() throws Exception {
        loadText("fun box() { val x = Array<Int>(5, { it } ).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testPrimitiveIterator() throws Exception {
        loadText("fun box() { val x = ByteArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.nextByte()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testPrimitiveIteratorByte() throws Exception {
        loadText("fun box() { for(x in ByteArray(5)) { java.lang.System.out?.println(x) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testPrimitiveIteratorShort() throws Exception {
        loadText("fun box() { for(x in ShortArray(5)) { java.lang.System.out?.println(x) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testPrimitiveIteratorInt() throws Exception {
        loadText("fun box() { for(x in IntArray(5)) { java.lang.System.out?.println(x) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testPrimitiveIteratorLong() throws Exception {
        loadText("fun box() { for(x in LongArray(5)) { java.lang.System.out?.println(x) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testPrimitiveIteratorFloat() throws Exception {
        loadText("fun box() { for(x in FloatArray(5)) { java.lang.System.out?.println(x) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testPrimitiveIteratorDouble() throws Exception {
        loadText("fun box() { for(x in DoubleArray(5)) { java.lang.System.out?.println(x) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testPrimitiveIteratorChar() throws Exception {
        loadText("fun box() { for(x in CharArray(5)) { java.lang.System.out?.println(x) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testPrimitiveIteratorBoolean() throws Exception {
        loadText("fun box() { for(x in BooleanArray(5)) { java.lang.System.out?.println(x) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testLongIterator() throws Exception {
        loadText("fun box() { val x = LongArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.nextLong()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testCharIterator() throws Exception {
        loadText("fun box() { val x = CharArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testByteIterator() throws Exception {
        loadText("fun box() { val x = ByteArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testShortIterator() throws Exception {
        loadText("fun box() { val x = ShortArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testIntIterator() throws Exception {
        loadText("fun box() { val x = IntArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testLongIterator2() throws Exception {
        loadText("fun box() { val x = LongArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testFloatIterator() throws Exception {
        loadText("fun box() { val x = FloatArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testDoubleIterator() throws Exception {
        loadText("fun box() { val x = ShortArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testBooleanIterator() throws Exception {
        loadText("fun box() { val x = BooleanArray(5).iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testArrayIndices() throws Exception {
        loadText(
                "fun box() { val x = Array<Int>(5, {it}).indices.iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testCharIndices() throws Exception {
        loadText("fun box() { val x = CharArray(5).indices.iterator(); while(x.hasNext()) { java.lang.System.out?.println(x.next()) } }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        foo.invoke(null);
    }

    public void testCollectionPlusAssign() throws Exception {
        blackBoxFile("regressions/kt33.kt");
    }

    public void testArrayPlusAssign() throws Exception {
        loadText("fun box() : Int { val s = IntArray(1); s [0] = 5; s[0] += 7; return s[0] }");
        //        System.out.println(generateToText());
        Method foo = generateFunction();
        assertTrue((Integer) foo.invoke(null) == 12);
    }

    public void testCollectionAssignGetMultiIndex() throws Exception {
        loadText("import java.util.ArrayList\n" +
                 "fun box() : String { val s = ArrayList<String>(1); s.add(\"\"); s [1, -1] = \"5\"; s[2, -2] += \"7\"; return s[2,-2] }\n" +
                 "fun ArrayList<String>.get(index1: Int, index2 : Int) = this[index1+index2]!!\n" +
                 "fun ArrayList<String>.set(index1: Int, index2 : Int, elem: String) { this[index1+index2] = elem }\n");
        //            System.out.println(generateToText());
        Method foo = generateFunction("box");
        assertTrue(foo.invoke(null).equals("57"));
    }

    public void testArrayGetAssignMultiIndex() throws Exception {
        loadText(
                "fun box() : String? { val s = Array<String>(1,{ \"\" }); s [1, -1] = \"5\"; s[2, -2] += \"7\"; return s[-3,3] }\n" +
                "fun Array<String>.get(index1: Int, index2 : Int) = this[index1+index2]\n" +
                "fun Array<String>.set(index1: Int, index2 : Int, elem: String) { this[index1+index2] = elem\n }");
        //        System.out.println(generateToText());
        Method foo = generateFunction("box");
        assertTrue(foo.invoke(null).equals("57"));
    }

    public void testCollectionGetMultiIndex() throws Exception {
        loadText("import java.util.ArrayList\n" +
                 "fun box() : String { val s = ArrayList<String>(1); s.add(\"\"); s [1, -1] = \"5\"; return s[2, -2] }\n" +
                 "fun ArrayList<String>.get(index1: Int, index2 : Int) = this[index1+index2]!!\n" +
                 "fun ArrayList<String>.set(index1: Int, index2 : Int, elem: String) { this[index1+index2] = elem }\n");
        //            System.out.println(generateToText());
        Method foo = generateFunction("box");
        assertTrue(foo.invoke(null).equals("5"));
    }

    public void testArrayGetMultiIndex() throws Exception {
        loadText(
                "fun box() : String? { val s = Array<String>(1,{ \"\" }); s [1, -1] = \"5\"; return s[-2, 2] }\n" +
                "fun Array<String>.get(index1: Int, index2 : Int) = this[index1+index2]\n" +
                "fun Array<String>.set(index1: Int, index2 : Int, elem: String) { this[index1+index2] = elem\n }");
        //        System.out.println(generateToText());
        Method foo = generateFunction("box");
        assertTrue(foo.invoke(null).equals("5"));
    }

    public void testMap() throws Exception {
        loadText(
                "fun box() : Int? { val s = java.util.HashMap<String,Int?>(); s[\"239\"] = 239; return s[\"239\"] }\n" +
                "fun java.util.HashMap<String,Int?>.set(index: String, elem: Int?) { this.put(index, elem) }");
        //        System.out.println(generateToText());
        Method foo = generateFunction("box");
        assertTrue((Integer) foo.invoke(null) == 239);
    }

    public void testLongDouble() throws Exception {
        loadText(
                "fun box() : Int { var l = IntArray(1); l[0.toLong()] = 4; l[0.toLong()] += 6; return l[0.toLong()];}\n" +
                "fun IntArray.set(index: Long, elem: Int) { this[index.toInt()] = elem }\n" +
                "fun IntArray.get(index: Long) = this[index.toInt()]");
        //        System.out.println(generateToText());
        Method foo = generateFunction("box");
        assertTrue((Integer) foo.invoke(null) == 10);
    }

    public void testKt503() {
        blackBoxFile("regressions/kt503.kt");
    }

    public void testKt602() {
        blackBoxFile("regressions/kt602.kt");
        //        System.out.println(generateToText());
    }

    public void testKt950() {
        blackBoxFile("regressions/kt950.kt");
    }

    public void testKt594() throws Exception {
        loadFile("regressions/kt594.kt");
        //        System.out.println(generateToText());
        blackBox();
    }

    public void testNonNullArray() throws Exception {
        blackBoxFile("classes/nonnullarray.kt");
        //        System.out.println(generateToText());
    }

    public void testArrayCast() throws Exception {
        blackBoxFile("regressions/kt2997.kt");
    }
}
