// ISSUE: KT-67912
// WITH_STDLIB

interface Bound

inline fun <reified F : Bound> foo(key: String): F? = null

fun main() {
    val value: Map<String, String> = <!INITIALIZER_TYPE_MISMATCH, NEW_INFERENCE_ERROR!>requireNotNull(
        foo("")
    )<!>
}
