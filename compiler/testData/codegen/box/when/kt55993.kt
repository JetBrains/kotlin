// CHECK_BYTECODE_TEXT

fun test(boolean: Boolean) {
    if (boolean) { // Breakpoint
        throw IllegalArgumentException()
    }
    val x: Int
}

fun box(): String {
    test(false)
    return "OK"
}

// 1 LINENUMBER 7 L1\n +ICONST_0\n +ISTORE 1
