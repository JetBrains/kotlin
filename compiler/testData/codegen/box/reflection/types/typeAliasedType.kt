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
    assertEquals("kotlin.String /* = test.T1 */", ::f1.returnType.toString())
    assertEquals("kotlin.collections.List<kotlin.Any> /* = test.T2<kotlin.Any> */", ::f2.returnType.toString())
    assertEquals("kotlin.collections.MutableMap<in kotlin.String /* = test.T1 */, kotlin.Int?> /* = test.T3<kotlin.Int, kotlin.String /* = test.T1 */> */", ::f3.returnType.toString())

    return "OK"
}
