// FIR_IDENTICAL
//!LANGUAGE: +DefinitelyNonNullableTypes

// NO_SIGNATURE_DUMP
// ^KT-57428

fun <T> (T & Any).foo() {}
fun <T> foo(l: (T & Any) -> Unit) {}

fun box() {
    "".foo<String?>()
    foo<String?> { "$it" }
}
