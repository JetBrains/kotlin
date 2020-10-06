// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// IGNORE_BACKEND: NATIVE
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

import kotlin.reflect.KType
import kotlin.reflect.typeOf

class C<T : Comparable<*>> {
    fun setOfT(): KType = typeOf<Set<T>>()
}

fun box(): String {
    val s = C<Int>().setOfT()
    return if (s.toString().endsWith("Set<T>")) "OK" else "Fail: $s"
}
