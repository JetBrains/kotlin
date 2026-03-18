// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR
// WITH_REFLECT
package test

import kotlin.test.assertEquals

class C<A, B> {
    class Nested
    inner class Inner(a: A)
}

fun box(): String {
    assertEquals("fun `<init>`(): test.C.Nested", C<*, *>::Nested.toString())
    assertEquals("fun test.C<A, B>.`<init>`(A): test.C<A, B>.Inner", C<*, *>::Inner.toString())

    assertEquals(C::class.typeParameters, C<*, *>::Inner.returnType.arguments.map { it.type?.classifier })

    return "OK"
}
