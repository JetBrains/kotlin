import kotlin.test.*

import kotlin.properties.*

private class NotNullVarTestGeneric<T : Any>(val a1: String, val b1: T) {
    var a: String by Delegates.notNull()
    var b by Delegates.notNull<T>()

    public fun doTest() {
        a = a1
        b = b1
        assertTrue(a == "a", "fail: a should be a, but was $a")
        assertTrue(b == "b", "fail: b should be b, but was $b")
    }
}

fun box() {
    NotNullVarTestGeneric("a", "b").doTest()
}