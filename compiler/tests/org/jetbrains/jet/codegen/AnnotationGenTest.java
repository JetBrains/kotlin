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

import jet.JetObject;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AnnotationGenTest extends CodegenTestCase {
    public void testPropField() throws NoSuchFieldException, NoSuchMethodException {
        loadText("[Deprecated] var x = 0");
        Class aClass = generateNamespaceClass();
        assertNull(aClass.getDeclaredMethod("getX").getAnnotation(Deprecated.class));
        assertNull(aClass.getDeclaredMethod("setX", int.class).getAnnotation(Deprecated.class));
        assertNotNull(aClass.getDeclaredField("x").getAnnotation(Deprecated.class));
    }

    public void testPropGetter() throws NoSuchFieldException, NoSuchMethodException {
        loadText("var x = 0\n" +
                 "[Deprecated] get");

        Class aClass = generateNamespaceClass();
        assertNotNull(aClass.getDeclaredMethod("getX").getAnnotation(Deprecated.class));
        assertNull(aClass.getDeclaredMethod("setX", int.class).getAnnotation(Deprecated.class));
        assertNull(aClass.getDeclaredField("x").getAnnotation(Deprecated.class));
    }

    public void testPropSetter() throws NoSuchFieldException, NoSuchMethodException {
        loadText("var x = 0\n" +
                 "[Deprecated] set");
//        System.out.println(generateToText());
        Class aClass = generateNamespaceClass();
        assertNull(aClass.getDeclaredMethod("getX").getAnnotation(Deprecated.class));
        assertNotNull(aClass.getDeclaredMethod("setX", int.class).getAnnotation(Deprecated.class));
        assertNull(aClass.getDeclaredField("x").getAnnotation(Deprecated.class));
    }

    public void testConstructor() throws NoSuchFieldException, NoSuchMethodException {
        loadText("class A [Deprecated] () {}");
        Class aClass = generateClass("A");
        Constructor x = aClass.getDeclaredConstructor();
        Deprecated annotation = (Deprecated) x.getAnnotation(Deprecated.class);
        assertNotNull(annotation);
    }

    public void testMethod() throws NoSuchFieldException, NoSuchMethodException {
        loadText("[Deprecated] fun x () {}");
        Class aClass = generateNamespaceClass();
        Method x = aClass.getDeclaredMethod("x");
        Deprecated annotation = (Deprecated) x.getAnnotation(Deprecated.class);
        assertNotNull(annotation);
    }

    public void testClass() throws NoSuchFieldException, NoSuchMethodException {
        loadText("[Deprecated] class A () {}");
        Class aClass = generateClass("A");
        Deprecated annotation = (Deprecated) aClass.getAnnotation(Deprecated.class);
        assertNotNull(annotation);
    }

    public void testSimplestAnnotationClass() throws NoSuchFieldException, NoSuchMethodException {
        loadText("annotation class A");
        Class aClass = generateClass("A");
        Class[] interfaces = aClass.getInterfaces();
        assertEquals(2, interfaces.length);
        assertEquals(0, aClass.getDeclaredMethods().length);
        assertTrue(Annotation.class == interfaces[0] || Annotation.class == interfaces[1]);
        assertTrue(JetObject.class == interfaces[0] || JetObject.class == interfaces[1]);
        assertTrue(aClass.isAnnotation());
    }

    public void testAnnotationClassWithStringProperty()
        throws
        NoSuchFieldException,
        NoSuchMethodException,
        ClassNotFoundException,
        IllegalAccessException,
        InstantiationException,
        InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "Retention(RetentionPolicy.RUNTIME) annotation class A(val a: String)\n" +
                 "" +
                 "A(\"239\") class B()");
        Class aClass = generateClass("A");

        Retention annotation = (Retention)aClass.getAnnotation(Retention.class);
        RetentionPolicy value = annotation.value();
        assertEquals(RetentionPolicy.RUNTIME, value);

        Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("a", methods[0].getName());
        assertEquals(String.class, methods[0].getReturnType());
        assertEquals(0, methods[0].getParameterTypes().length);
        assertTrue(aClass.isAnnotation());

        Class<?> bClass = aClass.getClassLoader().loadClass("B");
        Annotation bClassAnnotation = bClass.getAnnotation(aClass);
        assertNotNull(bClassAnnotation);

        assertEquals("239", methods[0].invoke(bClassAnnotation));
    }
}