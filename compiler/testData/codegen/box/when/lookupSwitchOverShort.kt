const val short100: Short = 100
const val short200: Short = 200
const val short300: Short = 300
const val short400: Short = 400

fun foo(p: Short): String {
    return when (p) {
        short100 -> "1"
        short200 -> "2"
        short300 -> "3"
        else -> "else"
    }
}

fun box(): String {
    return if (foo(short100) == "1"
        && foo(short200) == "2"
        && foo(short300) == "3"
        && foo(short400) == "else"
    ) "OK"
    else "FAIL"
}

// CHECK_BYTECODE_TEXT
// 1 LOOKUPSWITCH
// 0 TABLESWITCH