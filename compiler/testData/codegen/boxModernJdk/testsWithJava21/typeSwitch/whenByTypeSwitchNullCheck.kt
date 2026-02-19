// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 INVOKEDYNAMIC typeSwitch
// 1 java.lang.Void.class
// 1 java.lang.String.class
// 0 INSTANCEOF

fun test(k: Any?): Int {
    return when (k) {
        is Nothing -> 1
        null -> 0
        is String -> 2
        else -> 100
    }
}

fun box(): String {
    if (test(1.2) != 100) return "else"
    if (test("aa") != 2) return "String"
    if (test(null) != 0) return "null"

    return "OK"
}

