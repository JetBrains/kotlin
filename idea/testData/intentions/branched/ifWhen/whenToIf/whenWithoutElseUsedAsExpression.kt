//IS_APPLICABLE: false
enum class E { X, Y, Z}

fun test(e: E) {
    val i = <caret>when (e) {
        E.X -> 1
        E.Y -> 2
        E.Z -> 3
    }
}