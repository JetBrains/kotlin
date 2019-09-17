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

    consumeString(a())
    consumeString(a[1])

    val (x, y) = Example()
    consumeString(x)
    consumeString(y)

    consumeExample2(++a)

    consumeString(a plus b)
}

fun consumeInt(i: Int) {}
fun consumeString(s: String) {}
fun consumeExample2(e: Example2) {}