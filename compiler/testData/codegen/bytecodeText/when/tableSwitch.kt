fun foo(x: Int): String {
    return when (x) {
        101 -> "1"
        102 -> "2"
        103 -> "3"
        else -> "else"
    }
}

fun foo(p: Char): String {
    return when (p) {
        'a' -> "a"
        'b' -> "b"
        'c' -> "c"
        'd' -> "d"
        'e' -> "e"
        'f' -> "f"
        'g' -> "g"
        'h' -> "h"
        else -> "else"
    }
}

// 0 LOOKUPSWITCH
// 2 TABLESWITCH