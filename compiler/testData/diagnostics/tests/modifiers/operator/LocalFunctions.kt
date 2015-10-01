// !DIAGNOSTICS: -UNUSED_PARAMETER

class Example

fun Example.plus(other: Example) = 0
operator fun Example.minus(other: Example) = 0

operator fun Example.times(other: Example) = 0
fun Example.div(other: Example) = 0

fun a() {
    with (Example()) {
        operator fun Example.plus(other: Example) = ""
        fun Example.minus(other: Example) = ""

        operator fun Example.times(other: Example) = ""
        fun Example.div(other: Example) = ""

        with (Example()) {
            val a = Example()
            val b = Example()
            consumeString(a + b)
            consumeInt(a - b)
            a <!OVERLOAD_RESOLUTION_AMBIGUITY!>*<!> b
            a <!OVERLOAD_RESOLUTION_AMBIGUITY!>/<!> b
        }
    }
}

public fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

fun consumeInt(i: Int) {}
fun consumeString(s: String) {}