package org.jetbrains.jet.codegen;

import jet.NoPatternMatchedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author yole
 */
public class PatternMatchingTest extends CodegenTestCase {
    @Override
    protected String getPrefix() {
        return "patternMatching";
    }

    public void testConstant() throws Exception {
        loadFile();
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, 0));
        assertFalse((Boolean) foo.invoke(null, 1));
    }

    public void testExceptionOnNoMatch() throws Exception {
        loadFile();
        Method foo = generateFunction();
        assertTrue((Boolean) foo.invoke(null, 0));
        boolean caught = false;
        try {
            foo.invoke(null, 1);
        }
        catch(InvocationTargetException ex) {
            caught = ex.getTargetException() instanceof NoPatternMatchedException;
        }
        assertTrue(caught);
    }

    public void _testPattern() throws Exception {
        loadFile();
        Method foo = generateFunction();
        assertEquals("string", foo.invoke(null, ""));
        assertEquals("something", foo.invoke(null, new Object()));
    }

    public void testRange() throws Exception {
        loadFile();
        System.out.println(generateToText());
        Method foo = generateFunction();
        assertEquals("digit", foo.invoke(null, 9));
        assertEquals("something", foo.invoke(null, 19));
    }
}
