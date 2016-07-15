// ERROR: Cannot perform refactoring.\nCannot find a single definition to inline

fun f() {
    val v: Int
    if (true) {
        v = 239
    }
    else {
        v = 30
    }
    println(<caret>v)
}