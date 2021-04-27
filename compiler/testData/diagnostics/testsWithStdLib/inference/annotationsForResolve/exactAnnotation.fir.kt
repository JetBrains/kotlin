//!DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T, U: T> List<@kotlin.internal.Exact T>.firstTyped(): U = throw Exception()

fun test1(l: List<Number>) {

    val i: Int = l.firstTyped()

    val s: String = <!INITIALIZER_TYPE_MISMATCH, NEW_INFERENCE_ERROR!>l.firstTyped()<!>
}
