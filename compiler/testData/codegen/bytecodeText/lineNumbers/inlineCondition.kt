//FILE: test.kt
// IGNORE_BACKEND: JVM_IR

fun box() {
    if (listOf(1, 2, 3).myAny { it > 2 }) {
        println("foo")
    }
}

public inline fun <T> Iterable<T>.myAny(predicate: (T) -> Boolean): Boolean {
    for (element in this) {
        if (predicate(element)) return true
    }
    return false
}

// 3 LINENUMBER 5