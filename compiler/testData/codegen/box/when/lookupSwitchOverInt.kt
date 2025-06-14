fun foo(x: Int): String {
    return when (x) {
        100 -> "1"
        200 -> "2"
        300 -> "3"
        else -> "else"
    }
}

fun box(): String {
    return if (foo(100) == "1"
        && foo(200) == "2"
        && foo(300) == "3"
        && foo(42) == "else"
    ) "OK"
    else "FAIL"
}

// CHECK_BYTECODE_TEXT
// 1 LOOKUPSWITCH
// 0 TABLESWITCH