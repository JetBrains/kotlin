package d

//import from objects before properties resolve

import d.<!CANNOT_IMPORT_FROM_ELEMENT!>A<!>.*
import d.<!CANNOT_IMPORT_FROM_ELEMENT!>M<!>.R
import d.<!CANNOT_IMPORT_FROM_ELEMENT!>M<!>.R.bar
import d.<!CANNOT_IMPORT_FROM_ELEMENT!>M<!>.T
import d.<!CANNOT_IMPORT_FROM_ELEMENT!>M<!>.Y

var r: T = <!UNRESOLVED_REFERENCE!>T<!>()
val y: T = <!UNRESOLVED_REFERENCE!>Y<!>

fun f() {
    <!UNRESOLVED_REFERENCE!>bar<!>()
    <!UNRESOLVED_REFERENCE!>R<!>.bar()
    <!UNRESOLVED_REFERENCE!>B<!>.foo()
}

object M {
    object R {
        fun bar() {}
    }
    open class T() {}

    object Y : T() {}
}

object A {
    object B {
        fun foo() {}
    }
}