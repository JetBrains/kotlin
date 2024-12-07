// CHECK_BYTECODE_TEXT

fun test(boolean: Boolean) {
    val x: Int // Breakpoint
    if (boolean) {
        throw IllegalArgumentException()
    }
}

fun box(): String {
    test(false)
    return "OK"
}

// 1 LINENUMBER 4
