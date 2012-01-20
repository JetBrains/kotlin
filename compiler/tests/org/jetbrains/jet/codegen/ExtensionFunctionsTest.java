package org.jetbrains.jet.codegen;

import java.lang.reflect.Method;

/**
 * @author yole
 * @author alex.tkachman
 */
public class ExtensionFunctionsTest extends CodegenTestCase {
    @Override
    protected String getPrefix() {
        return "extensionFunctions";
    }

    public void testSimple() throws Exception {
        loadFile();
        final Method foo = generateFunction("foo");
        final Character c = (Character) foo.invoke(null);
        assertEquals('f', c.charValue());
    }

    public void testWhenFail() throws Exception {
        loadFile();
//        System.out.println(generateToText());
        Method foo = generateFunction("foo");
        assertThrows(foo, Exception.class, null, new StringBuilder());
    }

    public void testVirtual() throws Exception {
        blackBoxFile("extensionFunctions/virtual.jet");
    }

    public void testShared() throws Exception {
        blackBoxFile("extensionFunctions/shared.kt");
        System.out.println(generateToText());
    }

    public void testKt475() throws Exception {
        blackBoxFile("regressions/kt475.jet");
    }

    public void testKt865() throws Exception {
        createEnvironmentWithFullJdk();
        blackBoxFile("regressions/kt865.jet");
    }
}
