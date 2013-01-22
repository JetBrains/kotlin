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

    public void testKt238() {
        blackBoxFile("regressions/kt238.kt");
    }

    public void testKt326() {
        // Disabled: java.lang.ClassCastException: [Ljava.lang.Object; cannot be cast to [Ljava.lang.Integer;
        /*
        blackBoxFile("regressions/kt326.kt");
        */
    }

    public void testKt779() {
        blackBoxFile("regressions/kt779.kt");
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
        assertTrue(invoke instanceof Object[]);
    }

    public void testCreateMultiGenerics() throws Exception {
        // Disabled: java.lang.ClassCastException: [Ljava.lang.Object; cannot be cast to [Ljava.lang.Integer;
        /*
        loadText("class L<T>() { val a = Array<T?>(5) { null } } fun foo() = L<Int>().a");
        System.out.println(generateToText());
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        assertTrue(invoke.getClass() == Object[].class);
        */
    }

    public void testIntGenerics() throws Exception {
        loadText("class L<T>(var a : T) {} fun foo() = L<Int>(5).a");
        Method foo = generateFunction();
        Object invoke = foo.invoke(null);
        assertTrue(invoke instanceof Integer);
    }

    public void testIterator() {
        blackBoxFile("arrays/iterator.kt");
    }

    public void testIteratorLongArrayNextLong() {
        blackBoxFile("arrays/iteratorLongArrayNextLong.kt");
    }

    public void testIteratorByteArrayNextByte() {
        blackBoxFile("arrays/iteratorByteArrayNextByte.kt");
    }

    public void testForEachBooleanArray() {
        blackBoxFile("arrays/forEachBooleanArray.kt");
    }

    public void testForEachByteArray() {
        blackBoxFile("arrays/forEachByteArray.kt");
    }

    public void testForEachCharArray() {
        blackBoxFile("arrays/forEachCharArray.kt");
    }

    public void testForEachDoubleArray() {
        blackBoxFile("arrays/forEachDoubleArray.kt");
    }

    public void testForEachFloatArray() {
        blackBoxFile("arrays/forEachFloatArray.kt");
    }

    public void testForEachIntArray() {
        blackBoxFile("arrays/forEachIntArray.kt");
    }

    public void testForEachLongArray() {
        blackBoxFile("arrays/forEachLongArray.kt");
    }

    public void testForEachShortArray() {
        blackBoxFile("arrays/forEachShortArray.kt");
    }

    public void testIteratorBooleanArray() {
        blackBoxFile("arrays/iteratorBooleanArray.kt");
    }

    public void testIteratorByteArray() {
        blackBoxFile("arrays/iteratorByteArray.kt");
    }

    public void testIteratorCharArray() {
        blackBoxFile("arrays/iteratorCharArray.kt");
    }

    public void testIteratorDoubleArray() {
        blackBoxFile("arrays/iteratorDoubleArray.kt");
    }

    public void testIteratorFloatArray() {
        blackBoxFile("arrays/iteratorFloatArray.kt");
    }

    public void testIteratorIntArray() {
        blackBoxFile("arrays/iteratorIntArray.kt");
    }

    public void testIteratorLongArray() {
        blackBoxFile("arrays/iteratorLongArray.kt");
    }

    public void testIteratorShortArray() {
        blackBoxFile("arrays/iteratorShortArray.kt");
    }

    public void testArrayIndices() {
        blackBoxFile("arrays/indices.kt");
    }

    public void testIndicesChar() {
        blackBoxFile("arrays/indicesChar.kt");
    }

    public void testCollectionPlusAssign() {
        blackBoxFile("regressions/kt33.kt");
    }

    public void testArrayPlusAssign() {
        blackBoxFile("arrays/arrayPlusAssign.kt");
    }

    public void testCollectionAssignGetMultiIndex() {
        blackBoxFile("arrays/collectionAssignGetMultiIndex.kt");
    }

    public void testArrayGetAssignMultiIndex() {
        blackBoxFile("arrays/arrayGetAssignMultiIndex.kt");
    }

    public void testCollectionGetMultiIndex() {
        blackBoxFile("arrays/collectionGetMultiIndex.kt");
    }

    public void testArrayGetMultiIndex() {
        blackBoxFile("arrays/arrayGetMultiIndex.kt");
    }

    public void testHashMap() {
        blackBoxFile("arrays/hashMap.kt");
    }

    public void testLongAsIndex() {
        blackBoxFile("arrays/longAsIndex.kt");
    }

    public void testKt503() {
        blackBoxFile("regressions/kt503.kt");
    }

    public void testKt602() {
        blackBoxFile("regressions/kt602.kt");
    }

    public void testKt950() {
        blackBoxFile("regressions/kt950.kt");
    }

    public void testKt594() {
        blackBoxFile("regressions/kt594.kt");
    }

    public void testNonNullArray() {
        blackBoxFile("arrays/nonNullArray.kt");
    }

    public void testArrayCast() {
        blackBoxFile("regressions/kt2997.kt");
    }
}
