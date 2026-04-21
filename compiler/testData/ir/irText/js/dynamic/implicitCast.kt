// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// ISSUE: KT-85701
fun test() {
    val a: dynamic = 12
    assertEquals(60, a * 5)
}

fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
}
