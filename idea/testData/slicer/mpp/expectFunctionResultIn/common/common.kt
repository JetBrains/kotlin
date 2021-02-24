// FLOW: IN

expect fun foo(p: Any): Any

fun bar() {
    val result = foo(1)
    println(<caret>result)
}