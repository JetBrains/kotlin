annotation class Ann(val x: Int)

data class A(val x: Int, val y: Int)

fun bar(): Array<A> = null!!

fun foo() {
    for (@Ann(1) i in 1..100) {}
    for (@Ann(2) i in 1..100) {}

    for (<!WRONG_ANNOTATION_TARGET!>@Ann(3)<!> (<!UNUSED_VARIABLE!>x<!>, @Ann(4) <!UNUSED_VARIABLE!>y<!>) in bar()) {}

    for (@<!UNRESOLVED_REFERENCE!>Err<!>() (<!UNUSED_VARIABLE!>x<!>,<!UNUSED_VARIABLE!>y<!>) in bar()) {}
}
