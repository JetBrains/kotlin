package escapedNames

fun main() {
    val `one + one` = 1
    //Breakpoint!
    val a = 5
}

// EXPRESSION: `one + one` + 100
// RESULT: 101: I