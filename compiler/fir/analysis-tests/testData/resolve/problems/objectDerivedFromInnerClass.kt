class Outer { inner class Inner }
fun test() {
    val x = object : <!UNRESOLVED_REFERENCE!>Outer.Inner<!>() { }
}
