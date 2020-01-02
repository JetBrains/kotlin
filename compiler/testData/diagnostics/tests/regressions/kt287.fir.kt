// KT-287 Infer constructor type arguments

import java.util.*

fun attributes() : Map<String, String> = HashMap() // Should be inferred;
val attributes : Map<String, String> = HashMap() // Should be inferred;

fun foo(m : Map<String, String>) {}

fun test() {
    foo(HashMap())
}
