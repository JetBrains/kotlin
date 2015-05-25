// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(g: () -> Int) {}
fun foo(f: (Int) -> Int) {}

fun test() {
    foo { -> 42 }
}