// TARGET_BACKEND: JS_IR
// ISSUE: KT-90470

fun <T> assertEquals(a: T, b: T) {
    require(a == b) { "Assertion failed: $a != $b" }
}

fun foo() {
    val o: dynamic = js("({ n: 5 })")
    if (o.n is Int) o.n++
    assertEquals(6, o.n as Int)
}

fun bar() {
    val o: dynamic = js("({ n: 5 })")
    ++o.n
    assertEquals(6, o.n as Int)
    o.n++
    assertEquals(7, o.n as Int)
}

fun baz() {
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
