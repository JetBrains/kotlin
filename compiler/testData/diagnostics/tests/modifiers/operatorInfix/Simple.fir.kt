// !DIAGNOSTICS: -UNUSED_PARAMETER, -EXTENSION_SHADOWED_BY_MEMBER

open class Example {
    fun invoke() = 0
    fun get(i: Int) = 0

    fun component1() = 0
    fun component2() = 0

    fun inc() = Example()

    fun plus(o: Example) = 0
}

class Example2 : Example()

operator fun Example.invoke() = ""
operator fun Example.get(i: Int) = ""

operator fun Example.component1() = ""
operator fun Example.component2() = ""

operator fun Example.inc() = Example2()

infix fun Example.plus(o: Example) = ""

fun test() {
    var a = Example()
    val b = Example()

    <!INAPPLICABLE_CANDIDATE!>consumeString<!>(a())
    <!INAPPLICABLE_CANDIDATE!>consumeString<!>(a[1])

    val (x, y) = Example()
    <!INAPPLICABLE_CANDIDATE!>consumeString<!>(x)
    <!INAPPLICABLE_CANDIDATE!>consumeString<!>(y)

    <!INAPPLICABLE_CANDIDATE!>consumeExample2<!>(++a)

    <!INAPPLICABLE_CANDIDATE!>consumeString<!>(a plus b)
}

fun consumeInt(i: Int) {}
fun consumeString(s: String) {}
fun consumeExample2(e: Example2) {}