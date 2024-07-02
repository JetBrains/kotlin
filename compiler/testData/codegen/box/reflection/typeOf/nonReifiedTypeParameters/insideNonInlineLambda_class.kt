// TARGET_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_REFLECT

package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf

fun <K> g(k: K): KType =
    run2 {
        typeOf2(listOf(k))
    }

fun run2(f: () -> KType): KType = f()

inline fun <reified R> typeOf2(e: R): KType = typeOf<R>()

fun box(): String {
    val s = g("").toString()
    if (s != "kotlin.collections.List<K>") return "Fail: $s"

    return "OK"
}
