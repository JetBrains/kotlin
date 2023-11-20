// FIR_IDENTICAL
//!LANGUAGE: +DefinitelyNonNullableTypes

fun <T> (T & Any).foo() {}
fun <T> foo(l: (T & Any) -> Unit) {}

fun box() {
    "".foo<String?>()
    foo<String?> { "$it" }
}
