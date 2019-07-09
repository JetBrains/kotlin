package whenWithoutExpression

fun test(a: String): String {
    val b = a.toLowerCase()
    //Breakpoint!
    return when {
        b.startsWith("a") -> "A"
        b.startsWith("b") -> "B"
        b.startsWith("c") -> "C"
        else -> "other"
    }
}

fun main() {
    test("abcd")
}

// STEP_OVER: 4
// PRINT_FRAME