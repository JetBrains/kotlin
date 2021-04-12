package typedArray

fun main() {
    val list = listOf<CharSequence>("foo")
    //Breakpoint!
    val a = 5
}

// EXPRESSION: list.toTypedArray().size
// RESULT: 1: I
