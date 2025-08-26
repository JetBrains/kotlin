// WITH_STDLIB
// TARGET_BACKEND: JVM_IR, WASM

import kotlin.reflect.KClass

class C
annotation class KCls(
    val k1: KClass<*>,
    val k2: KClass<*>,
    val ks: Array<KClass<*>>
)

fun box(): String {
    val x = KCls(Int::class, IntArray::class, arrayOf(String::class, C::class))
    val y = KCls(Int::class, IntArray::class, arrayOf(String::class, C::class))
    val z = KCls(Int::class, IntArray::class, arrayOf(C::class, String::class))

    if (x != y) return "Fail1"

    if (x.hashCode() != y.hashCode()) return "Fail3"

    if (x == z) return "Fail2"

    return "OK"
}