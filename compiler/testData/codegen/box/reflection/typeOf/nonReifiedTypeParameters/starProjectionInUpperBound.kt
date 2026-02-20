// WITH_REFLECT
// WITH_STDLIB
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class C<T : Comparable<*>> {
    fun setOfT(): KType = typeOf<Set<T>>()
}

fun box(): String {
    val s = C<Int>().setOfT()
    return if (s.toString().endsWith("Set<T>")) "OK" else "Fail: $s"
}
