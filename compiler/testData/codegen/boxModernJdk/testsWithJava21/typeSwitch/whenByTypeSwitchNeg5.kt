import kotlin.collections.listOf

// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 0 INVOKEDYNAMIC typeSwitch

fun foo(x: Any?): Int {
    return when (x) {
        is String -> 1
        is String? -> 2
        else -> 4
    }
}

fun box(): String {
    if (foo("aa") != 1) return "1"
    if (foo(null) != 2) return "2"

    return "OK"
}