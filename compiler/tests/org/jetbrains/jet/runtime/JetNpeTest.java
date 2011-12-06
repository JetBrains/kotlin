package org.jetbrains.jet.runtime;

import jet.runtime.Intrinsics;
import org.jetbrains.jet.codegen.CodegenTestCase;

import java.lang.reflect.Method;

public class JetNpeTest extends CodegenTestCase {
    public void testStackTrace () {
        try {
            Intrinsics.throwNpe();
            fail("No Sure thrown");
        }
        catch (NullPointerException e) {
            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            assertEquals(stackTraceElement.getMethodName(), "testStackTrace");
            assertEquals(stackTraceElement.getClassName(), "org.jetbrains.jet.runtime.JetNpeTest");
        }
    }
    
    public void testNotNull () throws Exception {
        loadText("fun box() = if(10.sure() == 10) \"OK\" else \"fail\"");
        blackBox();
    }

    public void testNull () throws Exception {
        loadText("fun box() = if(null.sure() == 10) \"OK\" else \"fail\"");
//        System.out.println(generateToText());
        Method box = generateFunction("box");
        assertThrows(box, NullPointerException.class, null);
    }
}
