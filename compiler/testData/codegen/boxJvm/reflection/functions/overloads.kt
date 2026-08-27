// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.test.assertEquals

open class C {
    fun foo(x: String) {}

    suspend fun foo(y: Int) {}

    fun Long.foo() {}

    context(z: Number)
    fun foo() {}

    context(w: CharSequence)
    suspend fun foo() {}
}

class D : C()

fun box(): String {
    assertEquals(
        """
            context(w: kotlin.CharSequence) fun test.D.foo(): kotlin.Unit
            context(z: kotlin.Number) fun test.D.foo(): kotlin.Unit
            fun test.D.(kotlin.Long.)foo(): kotlin.Unit
            fun test.D.foo(kotlin.Int): kotlin.Unit
            fun test.D.foo(kotlin.String): kotlin.Unit
        """.trimIndent(),
        D::class.members.filter { it.name == "foo" }.sortedBy { it.toString() }.joinToString("\n"),
    )
    return "OK"
}
