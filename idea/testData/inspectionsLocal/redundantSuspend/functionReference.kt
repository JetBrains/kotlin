// PROBLEM: none
<caret>suspend fun f() {}

fun g(x: suspend () -> Unit) {}

fun test() {
    g(::f)
}