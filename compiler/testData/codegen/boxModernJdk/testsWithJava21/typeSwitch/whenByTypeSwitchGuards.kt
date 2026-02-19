// TARGET_BACKEND: JVM
// LANGUAGE: +WhenGuards
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 INVOKEDYNAMIC typeSwitch
// 2 INSTANCEOF

fun test(a: Any, b: Any) : String {
    val res1 =  when (b) {
        is String -> "O"
        is Int -> "Int Not OK"
        else -> "Else Not OK"
    }

    val res2 = when (b) {
        is String if b.length > 2 -> "K"
        is Int -> "Guard Int Not OK"
        else -> "Guard Else Not OK"
    }

    return res1 + res2
}

fun box(): String {
   return test("string", "secondString")
}