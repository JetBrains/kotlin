package org.jetbrains.jet.codegen;

import java.lang.reflect.Method;

/**
 * @author yole
 */
public class ClassGenTest extends CodegenTestCase {
    public void testPSVMClass() throws Exception {
        loadFile("simpleClass.jet");
        final Class aClass = generateClass();
        final Method[] methods = aClass.getMethods();
        assertEquals(1, methods.length);
    }
}
