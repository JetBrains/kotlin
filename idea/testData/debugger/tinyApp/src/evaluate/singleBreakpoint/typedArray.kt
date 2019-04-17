package typedArray

fun main() {
    val list = listOf<CharSequence>("foo")
    //Breakpoint!
    val a = 5
}

fun <T> block(block: () -> T): T {
    return block()
}

// EXPRESSION: list.toTypedArray().size
// RESULT: 1: I

// EXPRESSION: block { list.toTypedArray().size }
// RESULT: 1: I