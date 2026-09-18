// WITH_REFLECT
// ISSUE: KT-42913

import kotlin.reflect.*

inline fun <reified T> typeOfValue(x: T) = typeOf<T>()

inline fun <T> genericOfFunction(y: T): Pair<Any, KType> {
    val x = object {}
    return x to typeOfValue(listOf(x))
}

interface Interface

class Container<T> {
    fun genericOfClass(): Pair<Any, KType> {
        val obj = object : Interface {}
        return obj to typeOfValue(obj)
    }
}

fun box(): String {
    val systemProperties = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt")
    val useK1 = systemProperties.getMethod("getUseK1Implementation").invoke(null) == true
    if (useK1) return "OK" // This test does not run with K1 reflection

    val (x1, t1) = genericOfFunction(1)
    if (t1.classifier != List::class) return "FAIL 1: $t1"
    if (t1.arguments.single().type?.classifier != x1::class) return "FAIL 2: $t1"

    val (x2, t2) = Container<String>().genericOfClass()
    if (t2.classifier != x2::class) return "FAIL 3: $t2"

    return "OK"
}
