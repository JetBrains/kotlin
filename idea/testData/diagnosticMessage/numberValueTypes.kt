// !DIAGNOSTICS_NUMBER: 4
// !DIAGNOSTICS: TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS

package a

fun <T> id(t: T): T = t

fun <T> either(t1: T, t2: T): T = t1

fun <T> foo(l: List<T>) {}

fun <T> bar(t: T, l: MutableList<T>) {}

fun test(l: MutableList<String>) {
    val c: Int = id(9223372036854775807)

    val g: Byte = either(1, 300)

    foo(1)

    bar(1, l)
}