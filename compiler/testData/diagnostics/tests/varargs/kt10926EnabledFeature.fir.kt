// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EliminateAmbiguitiesWithExternalTypeParameters
// WITH_STDLIB

class AllCollection<T> {
    fun <K, T> addAll(vararg values: T, values2: Array<K>) = "OK" // 1
    fun <K, T> addAll(values: Array<K>, vararg values2: T) = 1 // 2
}

fun main(c: AllCollection<Any?>) {
    // KT-49620
    c.addAll(arrayOf(""), values2 = arrayOf(""))
}