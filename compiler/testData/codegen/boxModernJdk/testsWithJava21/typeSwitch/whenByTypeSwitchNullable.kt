// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 INVOKEDYNAMIC typeSwitch
// 0 INSTANCEOF

fun test(k: Any?): Int {
    return when (k) {
        is Int -> 1
        is String? -> 2
        null -> -1
        is String -> -2
        is String? -> -3
        else -> 100
    }
}

fun box(): String {
    if (test(1) != 1) return "1"
    if (test(null) != 2) return "null"
    if (test("aa") != 2) return "String"
    if (test(1.2) != 100) return "else"

    return "OK"
}

