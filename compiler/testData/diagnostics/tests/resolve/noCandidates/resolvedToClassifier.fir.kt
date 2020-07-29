// !DIAGNOSTICS: -UNUSED_VARIABLE

interface A

object B
class C

fun test() {
    val interface_as_fun = <!UNRESOLVED_REFERENCE!>A<!>()
    val interface_as_val = A

    val object_as_fun = <!HIDDEN!>B<!>()
    val class_as_val = C
}

fun <T> bar() {
    val typeParameter_as_val = <!OTHER_ERROR!>T<!>
    val typeParameter_as_fun = <!UNRESOLVED_REFERENCE!>T<!>()
}
