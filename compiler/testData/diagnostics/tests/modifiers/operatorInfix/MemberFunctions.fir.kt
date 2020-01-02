// !DIAGNOSTICS: -UNUSED_PARAMETER, -EXTENSION_SHADOWED_BY_MEMBER

class Example {
    <!INAPPLICABLE_INFIX_MODIFIER!>operator infix fun plus(other: Example) = 0<!>
    fun minus(other: Example) = 0

    <!INAPPLICABLE_INFIX_MODIFIER!>operator infix fun times(other: Example) = 0<!>
    fun div(other: Example) = 0
}

fun Example.plus(other: Example) = ""
operator infix fun Example.minus(other: Example) = ""

operator infix fun Example.times(other: Example) = ""
fun Example.div(other: Example) = ""

fun a() {
    val a = Example()
    val b = Example()

    a + b
    a - b
    a * b
    a / b

    a plus b
    a minus b
    a times b
    a div b

    with (Example()) {
        consumeInt(this + a)
        <!INAPPLICABLE_CANDIDATE!>consumeString<!>(this - b)
        consumeInt(this * a)
        consumeInt(this / b)

        consumeInt(this plus a)
        <!INAPPLICABLE_CANDIDATE!>consumeString<!>(this minus b)
        consumeInt(this times a)
        consumeInt(this div b)
    }
}

fun consumeInt(i: Int) {}
fun consumeString(s: String) {}