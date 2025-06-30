const val short101: Short = 101
const val short102: Short = 102
const val short103: Short = 103
const val short104: Short = 104

fun foo(p: Short): String {
    return when (p) {
        short101 -> "1"
        short102 -> "2"
        short103 -> "3"
        else -> "else"
    }
}

fun box(): String {
    return if (foo(short101) == "1"
        && foo(short102) == "2"
        && foo(short103) == "3"
        && foo(short104) == "else"
    ) "OK"
    else "FAIL"
}

// CHECK_BYTECODE_TEXT
// 0 LOOKUPSWITCH
// 1 TABLESWITCH