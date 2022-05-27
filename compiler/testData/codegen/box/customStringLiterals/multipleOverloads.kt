// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR
// WITH_STDLIB

object s
operator fun s.buildLiteral(body: LiteralBuilder.() -> Unit): String
        = LiteralBuilder().apply(body).build()

class LiteralBuilder {
    val sb = StringBuilder()

    // TODO figure out why 'operator' modifier is inapplicable there
    /* operator */ fun appendString(s: String) = sb.append(s)
    /* operator */ fun appendObject(x: Any) = sb.append(x)
    /* operator */ fun appendObject(x: Boolean) = sb.append(!x)
    /* operator */ fun appendObject(x: Int) = sb.append(x + 1)
    fun build(): String = sb.toString()
}

fun box(): String {
    val arg1 = 1
    val arg2 = true
    val arg3 = "abc"
    val expected = "string 2 false abc."
    val actual = s"string $arg1 ${arg2} $arg3."
    return if (expected == actual) "OK" else actual
}