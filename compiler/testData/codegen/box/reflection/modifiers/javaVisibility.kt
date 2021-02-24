// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

class J {
    protected class C {}
    protected static class D {}

    void foo() {}
    protected void bar() {}
    protected static void baz() {}
}

// FILE: K.kt

import kotlin.test.assertEquals

fun box(): String {
    // Package-private class
    assertEquals(null, J::class.visibility)
    // Protected+package class
    assertEquals(null, J.C::class.visibility)
    // Protected static class
    assertEquals(null, J.D::class.visibility)

    // Package-private method
    assertEquals(null, J::foo.visibility)
    // Protected+package method
    assertEquals(null, J::bar.visibility)
    // Protected static method
    assertEquals(null, J::baz.visibility)

    return "OK"
}
