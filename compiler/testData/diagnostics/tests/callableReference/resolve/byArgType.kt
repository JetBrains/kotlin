// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {}
fun foo(s: String) {}

fun fn(f: () -> Unit) {}

fun test() {
    fn(::foo)
}