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

fun box(): String {
    return if (foo('a') == "a"
        && foo('c') == "c"
        && foo('h') == "h"
        && foo('x') == "else"
    ) "OK"
    else "FAIL"
}

// CHECK_BYTECODE_TEXT
// 0 LOOKUPSWITCH
// 1 TABLESWITCH