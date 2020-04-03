// SKIP_TXT
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +NewInference

fun foo(f: () -> Unit) {}
fun bar(): Int = 42
fun test() {
    foo {
        <!UNUSED_EXPRESSION!>::bar<!> // should be fine
    }
    foo {
        <!UNUSED_LAMBDA_EXPRESSION!>{ "something" }<!> // should be fine
    }
}
