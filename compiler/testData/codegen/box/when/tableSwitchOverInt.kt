fun foo(x: Int): String {
    return when (x) {
        101 -> "1"
        102 -> "2"
        103 -> "3"
        else -> "else"
    }
}

fun box(): String {
    return if (foo(101) == "1"
        && foo(102) == "2"
        && foo(103) == "3"
        && foo(104) == "else"
    ) "OK"
    else "FAIL"
}

// CHECK_BYTECODE_TEXT
// 0 LOOKUPSWITCH
// 1 TABLESWITCH