fun String.f() {
    <!SUPER_NOT_AVAILABLE!>super@f<!>.compareTo("")
    <!SUPER_NOT_AVAILABLE!>super<!>.compareTo("")
}

fun foo() {
    <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>
    <!SUPER_NOT_AVAILABLE!>super<!>.foo()
    <!SUPER_NOT_AVAILABLE!>super<Nothing><!>.foo()
}

class A {
    fun act() {
        <!UNRESOLVED_REFERENCE!>println<!>("Test")
    }

    fun String.fact() {
        <!UNRESOLVED_REFERENCE!>println<!>("Fest")
    }
}
