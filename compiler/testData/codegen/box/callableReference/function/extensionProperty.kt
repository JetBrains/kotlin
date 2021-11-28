// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.reflect.KClass

fun box(): String {
    val arr: Array<KClass<*>> = arrayOf(String::class, Number::class) as Array<KClass<*>>
    val xs = arr.myMap { it.java }.toList()
    val ys = arr.myMap(KClass<*>::java).toList()
    if (xs != ys) return "fail1"
    if (!arr.foo()) return "fail2"
    return "OK"
}

public inline fun <A, B> Array<out A>.myMap(transform: (A) -> B): List<B> {
    return mapTo(ArrayList<B>(size), transform)
}

fun Any?.foo(): Boolean {
    val result = (this as Array<KClass<*>>).map(KClass<*>::java).toList()
    val withLambda = (this as Array<KClass<*>>).map { it.java }.toList()
    return result == withLambda
}