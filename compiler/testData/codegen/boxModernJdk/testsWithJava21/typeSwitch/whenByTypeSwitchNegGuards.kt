// TARGET_BACKEND: JVM
// LANGUAGE: +WhenGuards
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 0 INVOKEDYNAMIC typeSwitch
// 2 INSTANCEOF

fun test(b: Any) : String {
    return when (b) {
        is String if b.length > 2 -> "OK"
        is Int -> "Not OK"
        else -> "Else Not OK"
    }
}

fun box(): String {
    return test("string")
}