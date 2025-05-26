// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

typealias T1 = String
typealias T2<X> = List<X>
typealias T3<X, Y> = MutableMap<in Y, X?>

fun f1(): T1 = null!!
fun f2(): T2<Any> = null!!
fun f3(): T3<Int, T1> = null!!

fun box(): String {
    assertEquals("test.T1 /* = kotlin.String */", ::f1.returnType.toString())
    assertEquals("test.T2<kotlin.Any> /* = kotlin.collections.List<kotlin.Any> */", ::f2.returnType.toString())
    assertEquals("test.T3<kotlin.Int, test.T1 /* = kotlin.String */> /* = kotlin.collections.MutableMap<in test.T1 /* = kotlin.String */, kotlin.Int?> */", ::f3.returnType.toString())

    return "OK"
}
