// !WITH_NEW_INFERENCE
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
    requireString(<!ARGUMENT_TYPE_MISMATCH!><!INAPPLICABLE_CANDIDATE!>+<!> ExampleDeprecated()<!>)
}

fun requireInt(n: Int) {}
fun requireString(s: String) {}

class Example2 {
    operator fun plus() = this
    operator fun minus() = this

    fun test() {
        <!INAPPLICABLE_CANDIDATE!>+<!>this
        <!UNRESOLVED_REFERENCE!>-<!>this
    }
}
