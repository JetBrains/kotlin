package org.jetbrains.jet.runtime;

import jet.runtime.Intrinsics;
import junit.framework.TestCase;
import org.jetbrains.jet.codegen.CodegenTestCase;

import java.lang.reflect.Method;

public class JetNpeTest extends CodegenTestCase {
    public void testStackTrace () {
        try {
            Intrinsics.npe(null);
            fail("No NPE thrown");
        }
        catch (NullPointerException e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            assertEquals(stackTraceElement.getMethodName(), "testStackTrace");
            assertEquals(stackTraceElement.getClassName(), "org.jetbrains.jet.runtime.JetNpeTest");
        }
    }
    
    public void testNotNull () throws Exception {
        loadText("fun box() = if(10.npe() == 10) \"OK\" else \"fail\"");
        blackBox();
    }

    public void testNull () throws Exception {
        loadText("fun box() = if(null.npe() == 10) \"OK\" else \"fail\"");
        Method box = generateFunction("box");
        assertThrows(box, NullPointerException.class, null);
    }
}
