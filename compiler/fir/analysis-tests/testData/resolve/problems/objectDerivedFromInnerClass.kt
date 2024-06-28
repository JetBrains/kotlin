class Outer { open inner class Inner }
fun test() {
    val x = object : <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Outer.Inner<!>() { }
}
