// TARGET_BACKEND: JVM
// LANGUAGE: +WhenGuards
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 INVOKEDYNAMIC typeSwitch
// 2 INSTANCEOF

fun test(b: Any) : String {
    return when (b) {
        is String -> "O"
        is Int -> "Guard Double OK"
        else -> "Guard Triple OK"
    }
}

fun testGuard(b: Any) : String {
    return when (b) {
        is String if b.length > 2 -> "K"
        is Int -> "Guard Not OK"
        else -> "Guard Else Not OK"
    }
}

fun box(): String {
   return test("string") + testGuard("secondstring")
}