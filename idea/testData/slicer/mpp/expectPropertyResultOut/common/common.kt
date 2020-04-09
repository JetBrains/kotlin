// FLOW: OUT

expect val Int.property: Any

fun bar() {
    val result = <caret>1.property
    println(result)
}