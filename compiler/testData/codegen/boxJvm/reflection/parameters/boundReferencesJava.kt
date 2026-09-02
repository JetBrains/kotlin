// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J {
    public void foo(int x) {}
    public String bar = "";
}

// FILE: box.kt
import kotlin.test.assertEquals
import test.J

fun box(): String {
    val foo = J()::foo
    val bar = J()::bar

    assertEquals("[parameter #0 arg0 of fun test.J.foo(kotlin.Int): kotlin.Unit]", foo.parameters.toString())
    assertEquals("[]", bar.parameters.toString())
    assertEquals("[]", bar.getter.parameters.toString())
    assertEquals("[parameter #0 null of fun test.J.`<set-bar>`(kotlin.String!): kotlin.Unit]", bar.setter.parameters.toString())

    return "OK"
}
