// !DIAGNOSTICS_NUMBER: 3
// !DIAGNOSTICS: TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH
// !LANGUAGE: -NewInference

package s

fun <T> id(t: T): T = t

fun test(set: Set<Int>) {
    val s: String = id(1)

    val l: List<Int> = id(set)

    val ss: Set<String> = id(set)
}