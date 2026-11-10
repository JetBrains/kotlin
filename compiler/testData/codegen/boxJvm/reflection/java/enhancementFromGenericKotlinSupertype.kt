// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J implements KBase<String> {
    @Override
    public String foo() {
        return "OK";
    }

    @Override
    public void bar(String s) {
    }

    @Override
    public String baz() {
        return "OK";
    }
}

// FILE: box.kt
package test

import kotlin.test.assertEquals

interface KBase<T> {
    fun foo(): T
    fun bar(t: T)
    fun baz(): String
}

private fun check(expected: String, name: String) {
    assertEquals(expected, J::class.members.single { it.name == name }.toString())
}

fun box(): String {
    check("fun test.J.foo(): kotlin.String!", "foo")
    check("fun test.J.bar(kotlin.String!): kotlin.Unit", "bar")
    check("fun test.J.baz(): kotlin.String", "baz")

    return "OK"
}
