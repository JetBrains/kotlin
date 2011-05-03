package org.jetbrains.jet.codegen;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author yole
 */
public class ClassGenTest extends CodegenTestCase {
    public void testPSVMClass() throws Exception {
        loadFile("simpleClass.jet");
        System.out.println(generateToText());

        final Class aClass = loadClass("SimpleClass", generateClassesInFile());
        final Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
    }

    public void testArrayListInheritance() throws Exception {
        loadFile("inheritingFromArrayList.jet");
        System.out.println(generateToText());

        final Class aClass = loadClass("Foo", generateClassesInFile());
        checkInterface(aClass, List.class);
    }

    public void testInheritanceAndDelegation_DelegatingDefaultConstructorProperties() throws Exception {
        loadFile("inheritance.jet");
        System.out.println(generateToText());

        assertEquals("OK", blackBox());
    }

    private static void checkInterface(Class aClass, Class ifs) {
        for (Class anInterface : aClass.getInterfaces()) {
            if (anInterface == ifs) return;
        }
        fail(aClass.getName() + " must have " + ifs.getName() + " in its interfaces");
    }

    public void testNewInstanceExplicitConstructor() throws Exception {
        loadFile("newInstanceDefaultConstructor.jet");
        System.out.println(generateToText());
        final Codegens codegens = generateClassesInFile();
        loadImplementationClass(codegens, "SimpleClass");
        Class ns = loadRootNamespaceClass(codegens);
        final Method method = findMethodByName(ns, "test");
        final Integer returnValue = (Integer) method.invoke(null);
        assertEquals(610, returnValue.intValue());
    }
}
