// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.reflect.full.createType
import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.test.assertEquals

class A<T1> {
    inner class B<T2, T3> {
        inner class C<T4>
    }
    class D
}

fun foo(): A<Int>.B<Double, Float>.C<Long> = null!!

fun box(): String {
    fun KClass<*>.inv() = KTypeProjection.invariant(this.createType())

    val type = A.B.C::class.createType(listOf(Long::class.inv(), Double::class.inv(), Float::class.inv(), Int::class.inv()))
    assertEquals("A<kotlin.Int>.B<kotlin.Double, kotlin.Float>.C<kotlin.Long>", type.toString())

    assertEquals("A.D", A.D::class.createType().toString())

    return "OK"
}
