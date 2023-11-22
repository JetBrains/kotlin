// !DIAGNOSTICS: -UNUSED_VARIABLE

interface A

object B
class C

fun test() {
    val interface_as_fun = <!INTERFACE_AS_FUNCTION!>A<!>()
    val interface_as_val = <!NO_COMPANION_OBJECT!>A<!>

    val object_as_fun = <!UNRESOLVED_REFERENCE!>B<!>()
    val class_as_val = <!NO_COMPANION_OBJECT!>C<!>
}

fun <T> bar() {
    val typeParameter_as_val = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>
    val typeParameter_as_fun = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>()

    baz(<!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>)
    baz("$<!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>")

    1 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>

    B::class.equals(<!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>)

    <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION, VARIABLE_EXPECTED!>T<!> = ""
}

fun baz(a: Any) {}
