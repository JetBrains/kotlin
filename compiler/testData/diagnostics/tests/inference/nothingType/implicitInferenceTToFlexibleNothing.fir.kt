// !DIAGNOSTICS: -UNUSED_VARIABLE -UNCHECKED_CAST -UNUSED_PARAMETER
// !LANGUAGE: +NewInference
// SKIP_TXT

import java.util.*

fun <T> foo (f: () -> List<T>): T = null as T

fun main() {
    val x = foo { Collections.emptyList() }
}
