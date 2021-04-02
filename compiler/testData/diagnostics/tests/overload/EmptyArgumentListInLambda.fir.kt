// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(g: () -> Int) {}
fun foo(f: (Int) -> Int) {}

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> { -> 42 }
}