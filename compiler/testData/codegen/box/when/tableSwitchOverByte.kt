const val byte101: Byte = 101
const val byte102: Byte = 102
const val byte103: Byte = 103
const val byte104: Byte = 104

fun foo(p: Byte): String {
    return when (p) {
        byte101 -> "1"
        byte102 -> "2"
        byte103 -> "3"
        else -> "else"
    }
}

fun box(): String {
    return if (foo(byte101) == "1"
        && foo(byte102) == "2"
        && foo(byte103) == "3"
        && foo(byte104) == "else"
    ) "OK"
    else "FAIL"
}

// CHECK_BYTECODE_TEXT
// 0 LOOKUPSWITCH
// 1 TABLESWITCH