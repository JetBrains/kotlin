// ERROR: Cannot perform refactoring.\nCannot find a single definition to inline
fun f() {
    val v = if (true) a else b
    println(<caret>v++)
}