// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ KT-83349 Wrong hashCode values in instantiated annotations

import kotlin.reflect.KClass

enum class E { X, Y }

annotation class C(val id: Int)

annotation class Big(
    val ss: Array<String>,
    val ia: IntArray,
    val es: Array<E>,
    val ks: Array<KClass<*>>,
    val asA: Array<C>
)

class D

fun box(): String {
    val b1 = Big(
        ss = arrayOf("a","b"),
        ia = intArrayOf(1,2),
        es = arrayOf(E.X, E.Y),
        ks = arrayOf(String::class, D::class),
        asA = arrayOf(C(1), C(2))
    )
    val b2 = Big(
        ss = arrayOf("a","b"),
        ia = intArrayOf(1,2),
        es = arrayOf(E.X, E.Y),
        ks = arrayOf(String::class, D::class),
        asA = arrayOf(C(1), C(2))
    )

    val b3 = Big(
        ss = arrayOf("a","b"),
        ia = intArrayOf(1,2),
        es = arrayOf(E.Y, E.X),
        ks = arrayOf(String::class, D::class),
        asA = arrayOf(C(1), C(2))
    )

    if (b1 != b2) return "Failed"
    if (b1 == b3) return "Failed"

    return "OK"
}