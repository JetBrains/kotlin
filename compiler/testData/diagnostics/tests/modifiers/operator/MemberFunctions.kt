// !DIAGNOSTICS: -UNUSED_PARAMETER

class Example {
    operator fun plus(other: Example) = 0
    fun minus(other: Example) = 0

    operator fun times(other: Example) = 0
    fun div(other: Example) = 0
}

fun Example.plus(other: Example) = ""
operator fun Example.minus(other: Example) = ""

operator fun Example.times(other: Example) = ""
fun Example.div(other: Example) = ""

fun a() {
    val a = Example()
    val b = Example()
    a + b
    a - b
    a * b
    a <!OPERATOR_MODIFIER_REQUIRED!>/<!> b

    with (Example()) {
        consumeInt(this + a)
        consumeString(this - b)
        consumeInt(this * a)
        consumeInt(this <!OPERATOR_MODIFIER_REQUIRED!>/<!> b)
    }
}

public fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

fun consumeInt(i: Int) {}
fun consumeString(s: String) {}