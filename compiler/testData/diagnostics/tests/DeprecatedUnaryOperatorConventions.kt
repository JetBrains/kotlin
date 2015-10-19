// !DIAGNOSTICS: -UNUSED_PARAMETER

class Example {
    operator fun plus(): String = ""
    operator fun unaryPlus(): Int = 0
}

class ExampleDeprecated {
    operator fun plus(): String = ""
}

operator fun String.plus(): String = this
operator fun String.unaryPlus(): Int = 0

fun test() {
    requireInt(+ "")
    requireInt(+ Example())
    requireString(<!DEPRECATED_UNARY_PLUS_MINUS!>+ ExampleDeprecated()<!>)
}

fun requireInt(n: Int) {}
fun requireString(s: String) {}

class Example2 {
    operator fun plus() = this
    operator fun minus() = this

    fun test() {
        <!DEPRECATED_UNARY_PLUS_MINUS!>+this<!>
        <!DEPRECATED_UNARY_PLUS_MINUS!>-this<!>
    }
}