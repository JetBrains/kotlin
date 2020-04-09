// FLOW: OUT

expect fun foo(p: Any): Any

fun bar() {
    val result = foo(<caret>1)
    println(result)
}