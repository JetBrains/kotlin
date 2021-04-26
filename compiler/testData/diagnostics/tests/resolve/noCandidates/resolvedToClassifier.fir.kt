// !DIAGNOSTICS: -UNUSED_VARIABLE

interface A

object B
class C

fun test() {
    val interface_as_fun = <!UNRESOLVED_REFERENCE!>A<!>()
    val interface_as_val = A

    val object_as_fun = <!INVISIBLE_REFERENCE!>B<!>()
    val class_as_val = C
}

fun <T> bar() {
    val typeParameter_as_val = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>
    val typeParameter_as_fun = <!UNRESOLVED_REFERENCE!>T<!>()
}
