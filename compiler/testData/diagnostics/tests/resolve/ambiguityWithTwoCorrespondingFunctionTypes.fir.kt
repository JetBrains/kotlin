// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(a: Any, f: ()->Int) = f()
fun foo(a: Any, f: (Any)->Int) = f(a)
fun foo(i: Int, f: Int.()->Int) = i.f()

fun test1() {
    <!AMBIGUITY!>foo<!>(1) { ->
        this
    }
}
