// FLOW: IN

expect val Int.property: Any

fun bar() {
    val result = 1.property
    println(<caret>result)
}