// FLOW: OUT

expect fun Any.foo(): Any

fun bar() {
    val result = <caret>1.foo()
    println(result)
}