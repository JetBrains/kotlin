// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 0 INVOKEDYNAMIC typeSwitch

fun test(x: Any?, y: Any?): Int {
    return when {
        if (x == null) true else y is String -> 1
        y is Int -> 2
        else -> 3
    }
}

fun box(): String {
    if (test(null, Any()) != 1) return "x-null"
    if (test(1, "abc") != 1) return "string"
    if (test(1, 42) != 2) return "int"
    if (test(1, null) != 3) return "else"

    return "OK"
}
