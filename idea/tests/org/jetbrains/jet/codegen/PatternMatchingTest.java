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
}
