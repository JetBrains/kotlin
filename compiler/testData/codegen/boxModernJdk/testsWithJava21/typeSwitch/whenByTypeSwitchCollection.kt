import kotlin.collections.listOf

// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 0 INVOKEDYNAMIC typeSwitch


// TODO: Hmm. my tests shown it working but here it is a negative test #3.
// Try and check why

fun foo(x: Any): Int {
    return when (x) {
        is MutableList<*> -> 1
        is List<*> -> 2
        else -> 100
    }
}

fun box(): String {
    if (foo(mutableListOf(1)) != 1) return "1"
    if (foo(listOf<Int>()) != 2) return "2"

    return "OK"
}