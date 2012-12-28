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

public class TypeInfoTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected String getPrefix() {
        return "typeInfo";
    }

    public void testAsSafeOperator() throws Exception {
        loadText("fun foo(x: Any) = x as? Runnable");
        Method foo = generateFunction();
        assertNull(foo.invoke(null, new Object()));
        Runnable r = newRunnable();
        assertSame(r, foo.invoke(null, r));
    }

    public void testAsOperator() throws Exception {
        loadText("fun foo(x: Any) = x as Runnable");
        Method foo = generateFunction();
        Runnable r = newRunnable();
        assertSame(r, foo.invoke(null, r));
        assertThrows(foo, ClassCastException.class, null, new Object());
    }

    public void testIsOperator() throws Exception {
        loadText("fun foo(x: Any) = x is Runnable");
        Method foo = generateFunction();
        assertFalse((Boolean) foo.invoke(null, new Object()));
        assertTrue((Boolean) foo.invoke(null, newRunnable()));
    }

    public void testIsWithGenerics() throws Exception {
        // TODO: http://youtrack.jetbrains.net/issue/KT-612
        if (true) return;

        loadFile();
//        System.out.println(generateToText());
        Method foo = generateFunction();
        assertFalse((Boolean) foo.invoke(null));
    }

    public void testNotIsOperator() throws Exception {
        loadText("fun foo(x: Any) = x !is Runnable");
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, new Object()));
        assertFalse((Boolean) foo.invoke(null, newRunnable()));
    }

    public void testIsWithGenericParameters() throws Exception {
//  todo: obsolete with typeinfo removal
//        loadFile();
//        Method foo = generateFunction();
//        assertFalse((Boolean) foo.invoke(null));
    }

    public void testIsTypeParameter() throws Exception {
//  todo: obsolete with typeinfo removal
//        blackBoxFile("typeInfo/isTypeParameter.jet");
    }

    public void testAsSafeWithGenerics() throws Exception {
//  todo: obsolete with typeinfo removal
//        loadFile();
//        Method foo = generateFunction();
//        assertNull(foo.invoke(null));
    }

    public void testAsInLoop() throws Exception {
        loadFile();
        generateFunction();  // assert no exception
    }

    public void testPrimitiveTypeInfo() throws Exception {
        blackBoxFile("typeInfo/primitiveTypeInfo.jet");
    }

    public void testNullability() throws Exception {
//  todo: obsolete with typeinfo removal
//        blackBoxFile("typeInfo/nullability.jet");
    }

    public void testGenericFunction() throws Exception {
//  todo: obsolete with typeinfo removal
//        blackBoxFile("typeInfo/genericFunction.jet");
    }

    public void testForwardTypeParameter() throws Exception {
//  todo: obsolete with typeinfo removal
//        blackBoxFile("typeInfo/forwardTypeParameter.jet");
    }

    public void testClassObjectInTypeInfo() throws Exception {
        /*
        loadFile();
//        System.out.println(generateToText());
        Method foo = generateFunction();
        JetObject jetObject = (JetObject) foo.invoke(null);
        TypeInfo<?> typeInfo = jetObject.getTypeInfo();
        final Object object = typeInfo.getClassObject();
        final Method classObjFoo = object.getClass().getMethod("foo");
        assertNotNull(classObjFoo);
        */
    }

    private Runnable newRunnable() {
        return new Runnable() {
            @Override
            public void run() {
            }
        };
    }

    public void testKt259() throws Exception {
//  todo: obsolete with typeinfo removal
//        blackBoxFile("regressions/kt259.jet");
//        System.out.println(generateToText());
    }

    public void testKt511() throws Exception {
        blackBoxFile("regressions/kt511.jet");
//        System.out.println(generateToText());
    }

    public void testInner() throws Exception {
        blackBoxFile("typeInfo/inner.jet");
//        System.out.println(generateToText());
    }

    public void testKt2811() throws Exception {
        blackBoxFile("typeInfo/kt2811.kt");
    }

    public void testInheritance() throws Exception {
        blackBoxFile("typeInfo/inheritance.jet");
//        System.out.println(generateToText());
    }

    public void testkt1113() throws Exception {
//  todo: obsolete with typeinfo removal
//        blackBoxFile("regressions/kt1113.kt");
//        System.out.println(generateToText());
    }
}
