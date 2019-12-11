// !DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.reflect.KClass

fun test(s: String) {
    val f: () -> Int = s::hashCode
    val g: () -> String = s::toString
    val h: (Any?) -> Boolean = s::equals

    val k: KClass<out String> = s::class
    val l: KClass<*> = s::class
    val m: KClass<String> = String::class
    val n: KClass<Unit> = Unit::class
}
