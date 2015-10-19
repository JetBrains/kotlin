//!DIAGNOSTICS: -UNUSED_VARIABLE

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T, U: T> List<@kotlin.internal.Exact T>.firstTyped(): U = throw Exception()

fun test1(l: List<Number>) {

    val i: Int = l.firstTyped()

    val s: String = l.<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>firstTyped()<!>
}