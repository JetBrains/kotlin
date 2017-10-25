// !LANGUAGE: -ThrowNpeOnExplicitEqualsForBoxedNull
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt
import kotlin.test.*

fun box(): String {
    assertEquals(J.BOOL_NULL.equals(null), true)
    assertEquals(J.CHAR_NULL.equals(null), true)
    assertEquals(J.BYTE_NULL.equals(null), true)
    assertEquals(J.SHORT_NULL.equals(null), true)
    assertEquals(J.INT_NULL.equals(null), true)
    assertEquals(J.LONG_NULL.equals(null), true)
    assertEquals(J.FLOAT_NULL.equals(null), true)
    assertEquals(J.DOUBLE_NULL.equals(null), true)

    assertEquals(J.BOOL_NULL == null, true)
    assertEquals(J.CHAR_NULL == null, true)
    assertEquals(J.BYTE_NULL == null, true)
    assertEquals(J.SHORT_NULL == null, true)
    assertEquals(J.INT_NULL == null, true)
    assertEquals(J.LONG_NULL == null, true)
    assertEquals(J.FLOAT_NULL == null, true)
    assertEquals(J.DOUBLE_NULL == null, true)    
    
    return "OK"
}

// FILE: J.java
public class J {
    public static Boolean BOOL_NULL = null;
    public static Character CHAR_NULL = null;
    public static Byte BYTE_NULL = null;
    public static Short SHORT_NULL = null;
    public static Integer INT_NULL = null;
    public static Long LONG_NULL = null;
    public static Float FLOAT_NULL = null;
    public static Double DOUBLE_NULL = null;
}