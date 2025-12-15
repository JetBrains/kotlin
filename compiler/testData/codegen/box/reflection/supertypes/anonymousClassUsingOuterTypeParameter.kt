// TARGET_BACKEND: JVM
// WITH_REFLECT
package test

import kotlin.test.assertEquals

interface I<A, B>

fun <S, T> f() = object : I<T, S> {}

fun box(): String {
    // Ideally, we would have `test.I<T, S>`, where `T` and `S` are types obtained from f's type parameters.
    // But currently it's not implemented: KT-47030.
    // At the moment, this test checks that kotlin-reflect at least does not throw exception on such types.
    assertEquals("[test.I<???, ???>, kotlin.Any]", f<Any, Any>()::class.supertypes.toString())

    return "OK"
}
