// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
import java.lang.annotation.RetentionPolicy;

public class J {
    public static final boolean z = false;
    public static final char c = '1';
    public static final byte b = 0;
    public static final short s = 0;
    public static final int i = 0;
    public static final float f = 0f;
    public static final long j = 0L;
    public static final double d = 0.0;
    public static final String str = "X";

    public static final Object o = new Object();
    public static final Class<?> cl = Void.TYPE;
    public static final RetentionPolicy en = RetentionPolicy.RUNTIME;
}

// FILE: box.kt
import kotlin.test.assertFalse
import kotlin.test.assertTrue

fun box(): String {
    assertTrue(J::z.isConst)
    assertTrue(J::c.isConst)
    assertTrue(J::b.isConst)
    assertTrue(J::s.isConst)
    assertTrue(J::i.isConst)
    assertTrue(J::f.isConst)
    assertTrue(J::j.isConst)
    assertTrue(J::d.isConst)
    assertTrue(J::str.isConst)

    assertFalse(J::o.isConst)
    assertFalse(J::cl.isConst)
    assertFalse(J::en.isConst)

    return "OK"
}
