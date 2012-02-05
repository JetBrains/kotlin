package org.jetbrains.jet.codegen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
        System.out.println(generateToText());
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
}
