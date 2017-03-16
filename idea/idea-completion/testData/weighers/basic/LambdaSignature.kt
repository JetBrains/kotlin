fun <R> String.fold(initial: R, operation: (acc: R, Char) -> R): R = TODO()

fun foo(p: Int) {
    "abc".fold(1) { <caret> }
}

// ORDER: "acc, c ->"
// ORDER: "acc: Int, c: Char ->"
