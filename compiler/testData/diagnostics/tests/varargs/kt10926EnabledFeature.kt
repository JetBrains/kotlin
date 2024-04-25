// LANGUAGE: +EliminateAmbiguitiesWithExternalTypeParameters
// WITH_STDLIB

class AllCollection<T> {
    fun <K, T> addAll(vararg values: T, values2: Array<K>) = "OK" // 1
    fun <K, T> addAll(values: Array<K>, vararg values2: T) = 1 // 2
}

fun main(c: AllCollection<Any?>) {
    // KT-49620
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>c.addAll(arrayOf(""), values2 = arrayOf(""))<!>
}
