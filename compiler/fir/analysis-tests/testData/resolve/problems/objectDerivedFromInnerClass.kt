class Outer { inner class Inner }
fun test() {
    val x = object : <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>Outer.Inner<!>()<!> { }
}
