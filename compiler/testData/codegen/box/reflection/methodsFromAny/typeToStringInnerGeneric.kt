// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

package test

import kotlin.test.assertEquals

class A<T1> {
    inner class B<T2, T3> {
        inner class C<T4>
    }
}

fun foo(): A<Int>.B<Double, Float>.C<Long> = null!!

fun box(): String {
    assertEquals("test.A<kotlin.Int>.B<kotlin.Double, kotlin.Float>.C<kotlin.Long>", ::foo.returnType.toString())
    return "OK"
}
