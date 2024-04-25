// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(f: () -> T) {}
fun bar(g: () -> Unit) {}
fun <K> baz(): () -> K = TODO()

fun test() {
    foo { bar(baz()) }
}