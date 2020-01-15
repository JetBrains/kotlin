// SKIP_TXT
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty1
import kotlin.reflect.KFunction1

fun <T, R> has(property: KFunction1<T, R>) = null
fun <T, R> has(property: KProperty1<T, R>) = null

fun toInt(s: String) = 10

object A {
    fun main() {
        has(::toInt) // throwing an exception here
    }
}
