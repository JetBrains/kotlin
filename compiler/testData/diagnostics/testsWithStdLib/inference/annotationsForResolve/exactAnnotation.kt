//!DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T, U: T> List<@kotlin.internal.Exact T>.firstTyped(): U = throw Exception()

fun test1(l: List<Number>) {

    val i: Int = l.firstTyped()

    val s: String = <!TYPE_MISMATCH{NI}!>l.<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}!><!TYPE_MISMATCH{NI}!>firstTyped<!>()<!><!>
}
