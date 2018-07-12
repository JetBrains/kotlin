// PROBLEM: none
// WITH_RUNTIME
fun test() {
    <caret>with(foo()) {
        println("test")
    }
}

fun foo(): String {
    println("foo")
    return ""
}
