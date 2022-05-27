// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR
// WITH_STDLIB

object s
operator fun s.buildLiteral(body: LiteralBuilder.() -> Unit): Int = 0

class LiteralBuilder {
    // TODO figure out why 'operator' modifier is inapplicable there
    /* operator */ fun appendString(s: String) {}
    /* operator */ fun appendObject(x: Any) {}
}

fun box(): String {
    val arg1 = 1
    val arg2 = true
    val expected = 0
    val actual = s"any string with any args $arg1 $arg2"
    return if (expected == actual) "OK" else actual
}