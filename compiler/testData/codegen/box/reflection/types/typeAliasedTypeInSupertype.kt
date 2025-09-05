// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

typealias T1 = String
typealias T2<X> = List<X>
typealias T3<X, Y> = MutableMap<in Y, X?>

interface I<S>

class C1 : I<T1>
class C2 : I<T2<Any>>
class C3 : I<T3<Int, T1>>

fun box(): String {
    assertEquals("test.I<test.T1 /* = kotlin.String */>", C1::class.supertypes.first().toString())
    assertEquals("test.I<test.T2<kotlin.Any> /* = kotlin.collections.List<kotlin.Any> */>", C2::class.supertypes.first().toString())
    assertEquals("test.I<test.T3<kotlin.Int, test.T1 /* = kotlin.String */> /* = kotlin.collections.MutableMap<in test.T1 /* = kotlin.String */, kotlin.Int?> */>", C3::class.supertypes.first().toString())

    return "OK"
}
