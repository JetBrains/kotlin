fun foo(p: Char): String {
    return when (p) {
        'a' -> "a"
        'g' -> "g"
        'h' -> "h"
        else -> "else"
    }
}

fun box(): String {
    return if (foo('a') == "a"
        && foo('g') == "g"
        && foo('h') == "h"
        && foo('x') == "else"
    ) "OK"
    else "FAIL"
}

// CHECK_BYTECODE_TEXT
// 1 LOOKUPSWITCH
// 0 TABLESWITCH