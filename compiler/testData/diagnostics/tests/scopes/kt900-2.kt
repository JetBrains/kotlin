package d

//import from objects before properties resolve

import d.<!CANNOT_IMPORT_FROM_ELEMENT!>A<!>.*
import d.<!CANNOT_IMPORT_FROM_ELEMENT!>M<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>R<!>
import d.<!CANNOT_IMPORT_FROM_ELEMENT!>M<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>R<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>
import d.<!CANNOT_IMPORT_FROM_ELEMENT!>M<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>T<!>
import d.<!CANNOT_IMPORT_FROM_ELEMENT!>M<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>Y<!>

var r: T = <!UNRESOLVED_REFERENCE!>T<!>()
val y: T = <!UNRESOLVED_REFERENCE!>Y<!>

fun f() {
    <!UNRESOLVED_REFERENCE!>bar<!>()
    <!UNRESOLVED_REFERENCE!>R<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
    <!UNRESOLVED_REFERENCE!>B<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
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