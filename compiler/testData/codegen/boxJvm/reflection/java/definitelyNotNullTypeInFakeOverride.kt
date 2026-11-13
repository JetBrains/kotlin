// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J extends K<String> {
}

// FILE: test/G.java
package test;

public class G<X> extends K<X> {
}

// FILE: box.kt
package test

import kotlin.test.assertEquals

open class K<T> {
    fun foo(x: T & Any): T & Any = x
    fun bar(x: List<T & Any>) {}
}

fun box(): String {
    assertEquals("fun test.J.foo(kotlin.String): kotlin.String", J::class.members.single { it.name == "foo" }.toString())
    assertEquals("fun test.J.bar(kotlin.collections.List<kotlin.String>): kotlin.Unit", J::class.members.single { it.name == "bar" }.toString())

    assertEquals("fun test.G<X>.foo(X & Any): X & Any", G::class.members.single { it.name == "foo" }.toString())
    assertEquals("fun test.G<X>.bar(kotlin.collections.List<X & Any>): kotlin.Unit", G::class.members.single { it.name == "bar" }.toString())

    return "OK"
}
