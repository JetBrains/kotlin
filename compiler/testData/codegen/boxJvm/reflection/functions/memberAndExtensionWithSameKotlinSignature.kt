// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.test.assertEquals

open class A {
    fun String.foo(): String = "1"

    @JvmName("bar")
    fun foo(s: String): String = "2"
}

class B : A()

fun check(klass: KClass<*>) {
    val foos = klass.members.filter { it.name == "foo" }
    assertEquals(2, foos.size, "$klass: $foos")
    assertEquals(
        listOf("fun ${klass.simpleName}.(kotlin.String.)foo(): kotlin.String", "fun ${klass.simpleName}.foo(kotlin.String): kotlin.String"),
        foos.map { it.toString() }.sorted(),
    )
}

fun box(): String {
    check(A::class)
    check(B::class)
    return "OK"
}
