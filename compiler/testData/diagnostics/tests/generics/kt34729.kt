// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +NewInference

interface ILength {
    val length: Int
}

class Impl(override val length: Int) : ILength

fun <T> foo(a: (Int) -> T) = 0
fun <T : ILength> bar(a: (Int) -> T) {
    a(42).length
}

fun test() {
    foo<String> <!NI;TYPE_MISMATCH!>{ <!OI;TYPE_MISMATCH!><!>}<!>
    bar<Impl> <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>{ <!OI;TYPE_MISMATCH!><!>}<!>
}
