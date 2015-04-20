fun foo() {

}

class A {
    fun foo() {

    }

    class B
}

fun A.bar() {

}

fun test() {
    <!UNUSED_EXPRESSION!>::foo<!>
    <!UNUSED_EXPRESSION!>::A<!>
    <!UNUSED_EXPRESSION!>A::B<!>
    <!UNUSED_EXPRESSION!>A::foo<!>
    <!UNUSED_EXPRESSION!>A::bar<!>
}