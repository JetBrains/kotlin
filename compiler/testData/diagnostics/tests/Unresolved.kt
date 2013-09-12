package unresolved

class Pair<A, B>(val a: A, val b: B)

fun testGenericArgumentsCount() {
    val <!UNUSED_VARIABLE!>p1<!>: Pair<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!> = Pair(2, 2)
    val <!UNUSED_VARIABLE!>p2<!>: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Pair<!> = Pair(2, 2)
}

fun testUnresolved() {
    if (<!UNRESOLVED_REFERENCE!>a<!> is String) {
        val <!UNUSED_VARIABLE!>s<!> = <!UNRESOLVED_REFERENCE!>a<!>
    }
    <!UNRESOLVED_REFERENCE!>foo<!>(<!UNRESOLVED_REFERENCE!>a<!>)
    val s = "s"
    <!UNRESOLVED_REFERENCE!>foo<!>(s)
    foo1(<!UNRESOLVED_REFERENCE!>i<!>)
    s.<!UNRESOLVED_REFERENCE!>foo<!>()

    when(<!UNRESOLVED_REFERENCE!>a<!>) {
        is Int -> <!UNRESOLVED_REFERENCE!>a<!>
        is String -> <!UNRESOLVED_REFERENCE!>a<!>
    }

    for (j in <!UNRESOLVED_REFERENCE!>collection<!>) {
       var i: Int = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>j<!>
       i += 1
       foo1(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>j<!>)
    }
}

fun foo1(<!UNUSED_PARAMETER!>i<!>: Int) {}