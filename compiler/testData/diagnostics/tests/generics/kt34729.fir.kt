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
    foo<String> <!ARGUMENT_TYPE_MISMATCH!>{ }<!>
    bar<Impl> <!ARGUMENT_TYPE_MISMATCH!>{ }<!>
}
