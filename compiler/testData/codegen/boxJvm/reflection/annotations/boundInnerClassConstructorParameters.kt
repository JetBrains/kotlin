// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J {
    public class Inner {
        public Inner(@Anno("JInner1") String s, @Anno("JInner2") int x) {}
    }
}

// FILE: test/box.kt
package test

import kotlin.reflect.KCallable
import kotlin.test.assertEquals

annotation class Anno(val value: String)

class K {
    inner class Inner(@Anno("KInner1") s: String, @Anno("KInner2") x: Int)
}

private val KCallable<*>.parameterAnnotations: String
    get() = parameters.joinToString(", ") { p ->
        p.annotations.map { (it as Anno).value }.toString()
    }

fun box(): String {
    assertEquals("[], [JInner1], [JInner2]", J::Inner.parameterAnnotations)
    assertEquals("[], [KInner1], [KInner2]", K::Inner.parameterAnnotations)

    assertEquals("[JInner1], [JInner2]", J()::Inner.parameterAnnotations)
    assertEquals("[KInner1], [KInner2]", K()::Inner.parameterAnnotations)

    return "OK"
}
