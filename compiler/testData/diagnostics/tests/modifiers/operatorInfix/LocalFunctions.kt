// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Example

fun Example.plus(other: Example) = 0
operator infix fun Example.minus(other: Example) = 0

operator infix fun Example.times(other: Example) = 0
fun Example.div(other: Example) = 0

fun a() {
    with (Example()) {
        operator infix fun Example.plus(other: Example) = ""
        fun Example.minus(other: Example) = ""

        operator infix fun Example.times(other: Example) = ""
        fun Example.div(other: Example) = ""

        with (Example()) {
            val a = Example()
            val b = Example()

            consumeString(a + b)
            consumeInt(<!NI;TYPE_MISMATCH!>a <!NI;OPERATOR_MODIFIER_REQUIRED!>-<!> b<!>)

            consumeString(a plus b)
            consumeInt(a minus b)

            a * b
            a <!OPERATOR_MODIFIER_REQUIRED!>/<!> b

            a times b
            a <!INFIX_MODIFIER_REQUIRED!>div<!> b
        }
    }
}

fun consumeInt(i: Int) {}
fun consumeString(s: String) {}