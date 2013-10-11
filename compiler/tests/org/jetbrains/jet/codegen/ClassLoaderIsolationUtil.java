package org.jetbrains.jet.codegen;

import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import static junit.framework.Assert.*;

public class ClassLoaderIsolationUtil {

    public static void assertEquals(Class<?> expected, Class<?> actual) {
        if (actual == null) {
            assertNull(expected);
        }
        else Assert.assertEquals(getClassFromClassLoader(expected, actual.getClassLoader()), actual);
    }

    public static Class<? extends Annotation> getAnnotationClass(Class<? extends Annotation> annotationClass, ClassLoader classLoader) {
        return (Class<? extends Annotation>) getClassFromClassLoader(annotationClass, classLoader);
    }

    public static Object getAnnotationAttribute(Object annotation, String name) {
        try {
            return annotation.getClass().getMethod(name).invoke(annotation);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        return null;
    }

    @NotNull
    public static Class<?> getClassFromClassLoader(Class<?> classForName, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(classForName.getName());
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        return null;
    }
}
