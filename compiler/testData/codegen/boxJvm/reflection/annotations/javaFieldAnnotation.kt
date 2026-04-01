// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J {
    @Anno("sf")
    public static final String sf = "";
    @Anno("s")
    public static String s = "";
    @Anno("f")
    public final String f = "";
    @Anno("p")
    public String p = "";
}

// FILE: box.kt
package test

import kotlin.test.assertEquals

annotation class Anno(val value: String)

private fun List<Annotation>.toStr(): String =
    // `toString` of annotations on JDK 11+ has quotes around string literals. On JDK 17+, it doesn't have "value=".
    toString().replace("\"", "").replace("value=", "")

fun box(): String {
    assertEquals("[@test.Anno(sf)]", J::sf.annotations.toStr())
    assertEquals("[@test.Anno(s)]", J::s.annotations.toStr())
    assertEquals("[@test.Anno(f)]", J::f.annotations.toStr())
    assertEquals("[@test.Anno(p)]", J::p.annotations.toStr())

    assertEquals(emptyList(), J::sf.getter.annotations)
    assertEquals(emptyList(), J::s.getter.annotations)
    assertEquals(emptyList(), J::f.getter.annotations)
    assertEquals(emptyList(), J::p.getter.annotations)

    assertEquals(emptyList(), J::s.setter.annotations)
    assertEquals(emptyList(), J::p.setter.annotations)

    assertEquals(emptyList(), J::s.setter.parameters.last().annotations)
    assertEquals(emptyList(), J::p.setter.parameters.last().annotations)

    return "OK"
}
