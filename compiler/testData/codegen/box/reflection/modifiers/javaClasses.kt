// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: Interface.java
public interface Interface {
    int invoke(String s);
}

// FILE: J.java
public class J {
    public class Inner {}
    public static class Nested {}
}

// FILE: box.kt

import kotlin.test.assertTrue
import kotlin.test.assertFalse

fun box(): String {
    assertFalse(Interface::class.isData)
    assertFalse(Interface::class.isInner)
    assertFalse(Interface::class.isCompanion)
    assertFalse(Interface::class.isFun)
    assertFalse(Interface::class.isValue)

    assertFalse(J.Nested::class.isData)
    assertFalse(J.Nested::class.isInner)
    assertFalse(J.Nested::class.isCompanion)
    assertFalse(J.Nested::class.isFun)
    assertFalse(J.Nested::class.isValue)

    assertFalse(J.Inner::class.isData)
    assertTrue(J.Inner::class.isInner)
    assertFalse(J.Inner::class.isCompanion)
    assertFalse(J.Inner::class.isFun)
    assertFalse(J.Inner::class.isValue)

    return "OK"
}
