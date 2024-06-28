// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty1
import kotlin.reflect.KMutableProperty1

class Inv<T> {
    val size: Int = 0
}

class DTO<T> {
    val test: Inv<T>? = null
    var q: Int = 0
    operator fun <R> get(prop: KProperty1<*, R>): R = TODO()
    operator fun <R> set(prop: KMutableProperty1<*, R>, value: R) { }
}

fun main(intDTO: DTO<Int>?) {
    if (intDTO != null) {
        intDTO[DTO<Int>::q] = intDTO[DTO<Int>::test]!!.size
        intDTO[DTO<Int>::q] = intDTO[DTO<Int>::test]!!.size
    }
}
