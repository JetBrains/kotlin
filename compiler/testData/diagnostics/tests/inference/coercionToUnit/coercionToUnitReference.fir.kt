// SKIP_TXT
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +NewInference

fun foo(f: () -> Unit) {}
fun bar(): Int = 42
fun test() {
    foo {
        ::bar // should be fine
    }
    foo {
        { "something" } // should be fine
    }
}
