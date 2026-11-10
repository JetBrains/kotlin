// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public class J extends K<String> {
}

// FILE: G.java
public class G<X> extends K<X> {
}

// FILE: box.kt
import kotlin.test.assertEquals

open class K<T> {
    fun foo(x: T & Any): T & Any = x
    fun bar(x: List<T & Any>) {}
}

fun box(): String {
    assertEquals("fun J.foo(kotlin.String): kotlin.String", J::class.members.single { it.name == "foo" }.toString())
    assertEquals("fun J.bar(kotlin.collections.List<kotlin.String>): kotlin.Unit", J::class.members.single { it.name == "bar" }.toString())

    assertEquals("fun G<X>.foo(X & Any): X & Any", G::class.members.single { it.name == "foo" }.toString())
    assertEquals("fun G<X>.bar(kotlin.collections.List<X & Any>): kotlin.Unit", G::class.members.single { it.name == "bar" }.toString())

    return "OK"
}
