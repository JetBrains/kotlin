package unresolved

fun testGenericArgumentsCount() {
    val <!UNUSED_VARIABLE!>p1<!>: Tuple2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!> = #(2, 2)
    val <!UNUSED_VARIABLE!>p2<!>: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Tuple2<!> = #(2, 2)
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

    <!NO_ELSE_IN_WHEN!>when<!>(<!UNRESOLVED_REFERENCE!>a<!>) {
        is Int -> <!UNRESOLVED_REFERENCE!>a<!>
        is String -> <!UNRESOLVED_REFERENCE!>a<!>
    }

    for (j in <!UNRESOLVED_REFERENCE!>collection<!>) {
       var i: Int = j
       i += 1
       foo1(j)
    }
}

fun foo1(<!UNUSED_PARAMETER!>i<!>: Int) {}
