// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 0 INVOKEDYNAMIC typeSwitch

inline fun <reified T> foo(x: Any): Int {
    return when (x) {
        is T -> 1
        is Float -> 2
        else -> 0
    }
}

fun box(): String {
    if (foo<String>(1) != 0) return "0"
    if (foo<String>("abc") != 1) return "1"

    return "OK"
}