// !DIAGNOSTICS: -UNUSED_PARAMETER
fun Int.foo(x: Int) {
    js("this = x;")
}
