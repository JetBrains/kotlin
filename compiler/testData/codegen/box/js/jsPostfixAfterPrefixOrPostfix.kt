// TARGET_BACKEND: JS_IR
// ISSUE: KT-90470

fun <T> assertEquals(a: T, b: T) {
    require(a == b) { "Assertion failed: $a != $b" }
}

inline fun <reified E : Throwable> assertFailsWith(block: () -> Unit) {
    try { block() } catch (e: IllegalArgumentException) { return }
    throw kotlin.AssertionError("The lambda should have failed")
}

fun foo() = assertFailsWith<kotlin.AssertionError> {
    val o: dynamic = js("({ n: 5 })")
    if (o.n is Int) o.n++
    assertEquals(6, o.n as Int)
}

fun bar() = assertFailsWith<kotlin.AssertionError> {
    val o: dynamic = js("({ n: 5 })")
    ++o.n
    assertEquals(6, o.n as Int)
    o.n++
    assertEquals(7, o.n as Int)
}

fun baz() = assertFailsWith<kotlin.AssertionError> {
    val o: dynamic = js("({ n: 5 })")
    o.n++
    assertEquals(6, o.n as Int)
    o.n++
    assertEquals(7, o.n as Int)
}

fun box() = "OK".also {
    foo()
    bar()
    baz()
}
