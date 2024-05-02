// RENDER_DIAGNOSTICS_FULL_TEXT

fun foo(x: Int) {}

fun n() {
    foo(<!ARGUMENT_TYPE_MISMATCH!>{ a: String -> 42 }<!>)
}