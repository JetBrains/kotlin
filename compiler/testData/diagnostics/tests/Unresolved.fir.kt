package unresolved

class Pair<A, B>(val a: A, val b: B)

fun testGenericArgumentsCount() {
    val p1: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Pair<Int><!> = Pair(2, 2)
    val p2: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Pair<!> = Pair(2, 2)
}

fun testUnresolved() {
    if (<!UNRESOLVED_REFERENCE!>a<!> is String) {
        val s = <!UNRESOLVED_REFERENCE!>a<!>
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

    <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for (j in <!UNRESOLVED_REFERENCE!>collection<!>) {
       var i: Int = j
       i += 1
       foo1(j)
    }<!>
}

fun foo1(i: Int) {}