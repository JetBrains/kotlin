/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.jet.ConfigurationKind;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AnnotationGenTest extends CodegenTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
    }

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
        Class aClass = generateNamespaceClass();
        assertNull(aClass.getDeclaredMethod("getX").getAnnotation(Deprecated.class));
        assertNotNull(aClass.getDeclaredMethod("setX", int.class).getAnnotation(Deprecated.class));
        assertNull(aClass.getDeclaredField("x").getAnnotation(Deprecated.class));
    }

    public void testAnnotationForParamInGlobalFunction() throws NoSuchFieldException, NoSuchMethodException {
        loadText("fun x([Deprecated] i: Int) {}");
        Class aClass = generateNamespaceClass();
        Method x = aClass.getMethod("x", int.class);
        assertNotNull(x);
        // Get annotations for first parameter
        Annotation[] annotations = x.getParameterAnnotations()[0];
        assertNotNull(getDeprecatedAnnotationFromList(annotations));
    }

    public void testAnnotationForParamInLocalFunction() throws NoSuchFieldException, NoSuchMethodException {
        loadText("class A() { fun x([Deprecated] i: Int) {}}");
        Class aClass = generateClass("A");
        Method x = aClass.getMethod("x", int.class);
        assertNotNull(x);
        // Get annotations for first parameter
        Annotation[] annotations = x.getParameterAnnotations()[0];
        assertNotNull(getDeprecatedAnnotationFromList(annotations));
    }

    public void testParamInConstructor() throws NoSuchFieldException, NoSuchMethodException {
        loadText("class A ([Deprecated] x: Int) {}");
        Class aClass = generateClass("A");
        Constructor constructor = aClass.getDeclaredConstructor(int.class);
        assertNotNull(constructor);
        // Get annotations for first parameter
        Annotation[] annotations = constructor.getParameterAnnotations()[0];
        assertNotNull(getDeprecatedAnnotationFromList(annotations));
    }

    public void testPropFieldInConstructor() throws NoSuchFieldException, NoSuchMethodException {
        loadText("class A ([Deprecated] var x: Int) {}");
        Class aClass = generateClass("A");
        Constructor constructor = aClass.getDeclaredConstructor(int.class);
        assertNotNull(constructor);
        // Get annotations for first parameter
        Annotation[] annotations = constructor.getParameterAnnotations()[0];
        assertNotNull(getDeprecatedAnnotationFromList(annotations));
        assertNull(aClass.getDeclaredMethod("getX").getAnnotation(Deprecated.class));
        assertNull(aClass.getDeclaredMethod("setX", int.class).getAnnotation(Deprecated.class));
        assertNotNull(aClass.getDeclaredField("x").getAnnotation(Deprecated.class));
    }
    
    public void testAnnotationWithParamForParamInFunction() throws NoSuchFieldException, NoSuchMethodException {
        loadText("import java.lang.annotation.*\n" +
                 "Retention(RetentionPolicy.RUNTIME) annotation class A(val a: String)\n" +
                 "fun x(A(\"239\") i: Int) {}");
        Class aClass = generateNamespaceClass();
        Method x = aClass.getMethod("x", int.class);
        assertNotNull(x);
        // Get annotations for first parameter
        Annotation[] annotations = x.getParameterAnnotations()[0];
        Annotation resultAnnotation = null;
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().getCanonicalName().equals("A")) {
                resultAnnotation = annotation;
                break;
            }
        }
        assertNotNull(resultAnnotation);
    }

    private Deprecated getDeprecatedAnnotationFromList(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Deprecated) {
                return (Deprecated) annotation;
            }
        }
        return null;
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

    public void testAnnotationClassWithAnnotationProperty()
        throws
        NoSuchFieldException,
        NoSuchMethodException,
        ClassNotFoundException,
        IllegalAccessException,
        InstantiationException,
        InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "annotation class C(val c: String)\n" +
                 "Retention(RetentionPolicy.RUNTIME) annotation class A(val a: C)\n" +
                 "" +
                 "A(C(\"239\")) class B()");
        Class aClass = generateClass("A");

        Retention annotation = (Retention)aClass.getAnnotation(Retention.class);
        RetentionPolicy value = annotation.value();
        assertEquals(RetentionPolicy.RUNTIME, value);

        Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("a", methods[0].getName());
        assertEquals("C", methods[0].getReturnType().getName());
        assertEquals(0, methods[0].getParameterTypes().length);
        assertTrue(aClass.isAnnotation());

        Class<?> bClass = aClass.getClassLoader().loadClass("B");
        Annotation bClassAnnotation = bClass.getAnnotation(aClass);
        assertNotNull(bClassAnnotation);

        Object invoke = methods[0].invoke(bClassAnnotation);
        // there is some Proxy here
        Class<?> cClass = invoke.getClass().getInterfaces()[0];
        assertEquals("C", cClass.getName());
        assertEquals("239", cClass.getDeclaredMethod("c").invoke(invoke));
    }

    public void testAnnotationClassWithClassProperty()
            throws
            NoSuchFieldException,
            NoSuchMethodException,
            ClassNotFoundException,
            IllegalAccessException,
            InstantiationException,
            InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "Retention(RetentionPolicy.RUNTIME) annotation class A(val a: java.lang.Class<*>)\n" +
                 "" +
                 "A(javaClass<String>()) class B()");
        Class aClass = generateClass("A");

        Retention annotation = (Retention)aClass.getAnnotation(Retention.class);
        RetentionPolicy value = annotation.value();
        assertEquals(RetentionPolicy.RUNTIME, value);

        Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("a", methods[0].getName());
        assertEquals(Class.class, methods[0].getReturnType());
        assertEquals(0, methods[0].getParameterTypes().length);
        assertTrue(aClass.isAnnotation());

        Class<?> bClass = aClass.getClassLoader().loadClass("B");
        Annotation bClassAnnotation = bClass.getAnnotation(aClass);
        assertNotNull(bClassAnnotation);

        assertEquals(String.class, methods[0].invoke(bClassAnnotation));
    }

    public void testAnnotationClassWithStringArrayProperty()
            throws
            NoSuchFieldException,
            NoSuchMethodException,
            ClassNotFoundException,
            IllegalAccessException,
            InstantiationException,
            InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "Retention(RetentionPolicy.RUNTIME) annotation class A(val a: Array<String>)\n" +
                 "" +
                 "A(array(\"239\",\"932\")) class B()");
        Class aClass = generateClass("A");

        Retention annotation = (Retention)aClass.getAnnotation(Retention.class);
        RetentionPolicy value = annotation.value();
        assertEquals(RetentionPolicy.RUNTIME, value);

        Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("a", methods[0].getName());
        assertEquals(String[].class, methods[0].getReturnType());
        assertEquals(0, methods[0].getParameterTypes().length);
        assertTrue(aClass.isAnnotation());

        Class<?> bClass = aClass.getClassLoader().loadClass("B");
        Annotation bClassAnnotation = bClass.getAnnotation(aClass);
        assertNotNull(bClassAnnotation);

        Object invoke = methods[0].invoke(bClassAnnotation);
        assertEquals("239", ((String[])invoke)[0]);
        assertEquals("932", ((String[])invoke)[1]);
    }

    public void testAnnotationClassWithIntArrayProperty()
            throws
            NoSuchFieldException,
            NoSuchMethodException,
            ClassNotFoundException,
            IllegalAccessException,
            InstantiationException,
            InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "Retention(RetentionPolicy.RUNTIME) annotation class A(val a: IntArray)\n" +
                 "" +
                 "A(intArray(239,932)) class B()");
        Class aClass = generateClass("A");

        Retention annotation = (Retention)aClass.getAnnotation(Retention.class);
        RetentionPolicy value = annotation.value();
        assertEquals(RetentionPolicy.RUNTIME, value);

        Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("a", methods[0].getName());
        assertEquals(int[].class, methods[0].getReturnType());
        assertEquals(0, methods[0].getParameterTypes().length);
        assertTrue(aClass.isAnnotation());

        Class<?> bClass = aClass.getClassLoader().loadClass("B");
        Annotation bClassAnnotation = bClass.getAnnotation(aClass);
        assertNotNull(bClassAnnotation);

        Object invoke = methods[0].invoke(bClassAnnotation);
        assertEquals(239, ((int[])invoke)[0]);
        assertEquals(932, ((int[])invoke)[1]);
    }

    public void testAnnotationClassWithEnumArrayProperty()
            throws
            NoSuchFieldException,
            NoSuchMethodException,
            ClassNotFoundException,
            IllegalAccessException,
            InstantiationException,
            InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "Target(ElementType.TYPE, ElementType.METHOD) annotation class A");
        Class aClass = generateClass("A");

        Target annotation = (Target)aClass.getAnnotation(Target.class);
        ElementType[] value = annotation.value();
        assertEquals(2, value.length);

        assertEquals(ElementType.TYPE, value[0]);
        assertEquals(ElementType.METHOD, value[1]);
    }

    public void testAnnotationClassWithAnnotationArrayProperty()
            throws
            NoSuchFieldException,
            NoSuchMethodException,
            ClassNotFoundException,
            IllegalAccessException,
            InstantiationException,
            InvocationTargetException {
        loadText("import java.lang.annotation.*\n" +
                 "" +
                 "Retention(RetentionPolicy.RUNTIME) annotation class A(val a: Array<Retention>)\n" +
                 "" +
                 "A(array(Retention(RetentionPolicy.RUNTIME),Retention(RetentionPolicy.SOURCE))) class B()");
        Class aClass = generateClass("A");

        Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("a", methods[0].getName());

        Class<?> bClass = aClass.getClassLoader().loadClass("B");
        Annotation bClassAnnotation = bClass.getAnnotation(aClass);
        assertNotNull(bClassAnnotation);

        Object invoke = methods[0].invoke(bClassAnnotation);
        Retention[] invoke1 = (Retention[])invoke;
        assertEquals(2, invoke1.length);
        assertEquals(invoke1[0].value(), RetentionPolicy.RUNTIME);
        assertEquals(invoke1[1].value(), RetentionPolicy.SOURCE);
    }
}
