package org.jetbrains.jet.codegen;

import java.lang.reflect.Method;

/**
 * @author yole
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
        Method foo = generateFunction("foo");
        assertThrows(foo, Exception.class, null, new StringBuilder());
    }

    public void testGeneric() throws Exception {
        blackBoxFile("extensionFunctions/generic.jet");
    }
}
