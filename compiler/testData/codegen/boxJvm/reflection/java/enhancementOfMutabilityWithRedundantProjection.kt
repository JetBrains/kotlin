// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

import java.util.Collection;
import java.util.List;

public class J implements K {
    @Override
    public void foo(Collection<? extends CharSequence> c) {
    }

    @Override
    public void bar(List<? extends CharSequence> l) {
    }

    @Override
    public void baz(Collection<? extends CharSequence> c) {
    }
}

// FILE: box.kt
package test

import kotlin.test.assertEquals

interface K {
    fun foo(c: Collection<CharSequence>)
    fun bar(l: List<CharSequence>)
    fun baz(c: MutableCollection<out CharSequence>)
}

private fun check(expected: String, name: String) {
    assertEquals(expected, J::class.members.single { it.name == name }.toString())
}

fun box(): String {
    check("fun test.J.foo(kotlin.collections.Collection<kotlin.CharSequence>): kotlin.Unit", "foo")
    check("fun test.J.bar(kotlin.collections.List<kotlin.CharSequence>): kotlin.Unit", "bar")

    check(
        "fun test.J.baz((kotlin.collections.MutableCollection<out kotlin.CharSequence>..kotlin.collections.MutableCollection<kotlin.CharSequence>)): kotlin.Unit",
        "baz",
    )

    return "OK"
}
