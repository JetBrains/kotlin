// !DIAGNOSTICS: -UNUSED_PARAMETER

class Example {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plus(): String = ""
    operator fun unaryPlus(): Int = 0
}

class ExampleDeprecated {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plus(): String = ""
}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun String.plus(): String = this
operator fun String.unaryPlus(): Int = 0

fun test() {
    requireInt(+ "")
    requireInt(+ Example())
    requireString(<!ARGUMENT_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+<!> ExampleDeprecated()<!>)
}

fun requireInt(n: Int) {}
fun requireString(s: String) {}

class Example2 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plus() = this
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun minus() = this

    fun test() {
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+<!>this
        <!UNRESOLVED_REFERENCE!>-<!>this
    }
}
