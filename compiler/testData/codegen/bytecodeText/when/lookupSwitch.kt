fun foo(x: Int): String {
    return when (x) {
        100 -> "1"
        200 -> "2"
        300 -> "3"
        else -> "else"
    }
}

fun foo(p: Char): String {
    return when (p) {
        'a' -> "a"
        'g' -> "g"
        'h' -> "h"
        else -> "else"
    }
}

// 2 LOOKUPSWITCH
// 0 TABLESWITCH